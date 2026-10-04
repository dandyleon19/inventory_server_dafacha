package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.model.DashboardSummary;
import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.model.StockSummary;
import com.dafacha.inventory.domain.model.WarehouseValuation;
import com.dafacha.inventory.domain.ports.in.DashboardUseCase;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.domain.ports.out.PurchaseOrderRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardUseCase {

    /** How many rows each dashboard widget shows. */
    private static final int LIST_SIZE = 5;

    private final StockRepositoryPort stockRepositoryPort;
    private final StockLotRepositoryPort lotRepositoryPort;
    private final StockMovementRepositoryPort movementRepositoryPort;
    private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final CompanySettingsRepositoryPort settingsRepositoryPort;

    private record Counts(long activeProducts, long warehouses, long lowStock, long expiredLots,
                          long expiringLots, long arriving, long overdue) {
    }

    private record Lists(List<WarehouseValuation> warehouses, List<StockSummary> lowStock,
                         List<StockLot> expiringLots, List<PurchaseOrder> arrivingOrders,
                         List<StockMovement> recentMovements) {
    }

    @Override
    public Mono<DashboardSummary> getSummary() {
        return SecurityHelper.currentTenantUser().flatMap(user -> {
            Long companyId = user.getCompanyId();
            // null = no restriction; otherwise every figure below covers only the user's warehouses.
            List<Long> allowed = user.getWarehouseIds();

            return settingsRepositoryPort.findByCompanyId(companyId)
                        .defaultIfEmpty(CompanySettings.defaults(companyId))
                        .flatMap(settings -> {
                            int expiryDays = orDefault(settings.getExpiryAlertDays(), 30);
                            int arrivalDays = orDefault(settings.getPurchaseArrivalAlertDays(), 7);

                            Mono<Counts> counts = Mono.zip(
                                            productRepositoryPort.count(companyId, null, true, null),
                                            allowed == null
                                                    ? warehouseRepositoryPort.count(companyId, true, null)
                                                    : Mono.just((long) allowed.size()),
                                            stockRepositoryPort.countSummary(companyId, null, null, allowed, true, null, true),
                                            lotRepositoryPort.count(companyId, null, null, allowed, LotScope.EXPIRED, expiryDays),
                                            lotRepositoryPort.count(companyId, null, null, allowed, LotScope.EXPIRING, expiryDays),
                                            purchaseOrderRepositoryPort.countArriving(companyId, allowed, arrivalDays),
                                            purchaseOrderRepositoryPort.countOverdue(companyId, allowed))
                                    .map(t -> new Counts(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6(), t.getT7()));

                            Mono<Lists> lists = Mono.zip(
                                            stockRepositoryPort.findValuation(companyId, allowed).collectList(),
                                            stockRepositoryPort.findSummaryPage(companyId, null, null, allowed, true, null, true, 0, LIST_SIZE).collectList(),
                                            lotRepositoryPort.findPage(companyId, null, null, allowed, LotScope.ATTENTION, expiryDays, 0, LIST_SIZE).collectList(),
                                            purchaseOrderRepositoryPort.findArriving(companyId, allowed, arrivalDays, LIST_SIZE).collectList(),
                                            movementRepositoryPort.findPage(companyId, null, null, allowed, null, null, 0, LIST_SIZE).collectList())
                                    .map(t -> new Lists(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5()));

                            return Mono.zip(counts, lists).map(t -> {
                                Counts c = t.getT1();
                                Lists l = t.getT2();
                                return DashboardSummary.builder()
                                        .totalValue(l.warehouses().stream()
                                                .map(WarehouseValuation::getValue)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                                        .activeProductCount(c.activeProducts())
                                        .warehouseCount(c.warehouses())
                                        .lowStockCount(c.lowStock())
                                        .expiredLotCount(c.expiredLots())
                                        .expiringLotCount(c.expiringLots())
                                        .arrivingOrderCount(c.arriving())
                                        .overdueOrderCount(c.overdue())
                                        .expiryAlertDays(expiryDays)
                                        .purchaseArrivalAlertDays(arrivalDays)
                                        .warehouses(l.warehouses())
                                        .lowStock(l.lowStock())
                                        .expiringLots(l.expiringLots())
                                        .arrivingOrders(l.arrivingOrders())
                                        .recentMovements(l.recentMovements())
                                        .build();
                            });
                        });
        });
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
