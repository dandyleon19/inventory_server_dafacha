package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.response.DashboardResponse;
import com.dafacha.inventory.domain.model.DashboardSummary;
import com.dafacha.inventory.domain.ports.in.DashboardUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.PurchaseOrderMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMovementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;
    private final StockMapper stockMapper;
    private final StockMovementMapper stockMovementMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    /** Valuation, alert counters and short lists. Also feeds the alerts bell in the top bar. */
    @GetMapping
    public Mono<ResponseEntity<DashboardResponse>> getDashboard() {
        return dashboardUseCase.getSummary()
                .map(this::toResponse)
                .map(ResponseEntity::ok);
    }

    private DashboardResponse toResponse(DashboardSummary summary) {
        return DashboardResponse.builder()
                .totalValue(summary.getTotalValue())
                .activeProductCount(summary.getActiveProductCount())
                .warehouseCount(summary.getWarehouseCount())
                .lowStockCount(summary.getLowStockCount())
                .expiredLotCount(summary.getExpiredLotCount())
                .expiringLotCount(summary.getExpiringLotCount())
                .arrivingOrderCount(summary.getArrivingOrderCount())
                .overdueOrderCount(summary.getOverdueOrderCount())
                .expiryAlertDays(summary.getExpiryAlertDays())
                .purchaseArrivalAlertDays(summary.getPurchaseArrivalAlertDays())
                .warehouses(summary.getWarehouses().stream().map(stockMapper::toResponse).toList())
                .lowStock(summary.getLowStock().stream().map(stockMapper::toResponse).toList())
                .expiringLots(summary.getExpiringLots().stream().map(stockMapper::toResponse).toList())
                .arrivingOrders(summary.getArrivingOrders().stream().map(purchaseOrderMapper::toResponse).toList())
                .recentMovements(summary.getRecentMovements().stream().map(stockMovementMapper::toResponse).toList())
                .build();
    }
}
