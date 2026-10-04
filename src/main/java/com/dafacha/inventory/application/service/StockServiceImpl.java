package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.domain.exception.BadRequestException;
import com.dafacha.inventory.domain.exception.ForbiddenException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.exception.UnauthorizedException;
import com.dafacha.inventory.domain.model.CompanySettings;
import com.dafacha.inventory.domain.model.CostLayer;
import com.dafacha.inventory.domain.model.LayerPick;
import com.dafacha.inventory.domain.model.InventoryValuation;
import com.dafacha.inventory.domain.model.LotCount;
import com.dafacha.inventory.domain.model.LotPick;
import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.NewLot;
import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.model.StockOperation;
import com.dafacha.inventory.domain.model.StockSummary;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.model.WarehouseValuation;
import com.dafacha.inventory.domain.ports.in.StockUseCase;
import com.dafacha.inventory.domain.ports.out.CompanySettingsRepositoryPort;
import com.dafacha.inventory.domain.ports.out.CostLayerRepositoryPort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockLotRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockMovementRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import com.dafacha.inventory.domain.ports.out.TransactionPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StockServiceImpl implements StockUseCase {

    private final StockRepositoryPort stockRepositoryPort;
    private final StockMovementRepositoryPort movementRepositoryPort;
    private final StockLotRepositoryPort lotRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final CompanySettingsRepositoryPort settingsRepositoryPort;
    private final TransactionPort transactionPort;
    private final StockLedger stockLedger;
    private final CostLayerRepositoryPort costLayerRepositoryPort;

    // ---------------------------------------------------------------- operations

    @Override
    public Mono<List<StockMovement>> registerEntry(StockOperation op) {
        return inTransaction(ctx -> Mono.zip(
                        findActiveWarehouse(ctx.companyId(), op.getWarehouseId()),
                        findProduct(ctx.companyId(), op.getProductId()))
                .flatMap(refs -> {
                    requireWholeIfNeeded(refs.getT2(), op.getQuantity());
                    StockLedger.Change change = change(MovementType.ENTRY, op.getQuantity(), op.getNotes(), null, op.getUnitCost());
                    return stockLedger.credit(ctx, refs.getT1(), refs.getT2(), change, lotOf(op), true);
                })
                .map(StockLedger.Result::movements));
    }

    @Override
    public Mono<List<StockMovement>> registerExit(StockOperation op) {
        return inTransaction(ctx -> Mono.zip(
                        findActiveWarehouse(ctx.companyId(), op.getWarehouseId()),
                        findProduct(ctx.companyId(), op.getProductId()))
                .flatMap(refs -> {
                    requireWholeIfNeeded(refs.getT2(), op.getQuantity());
                    requireValidPicks(refs.getT2(), op.getQuantity(), op.getLots());
                    requireValidLayerPicks(refs.getT2(), op.getQuantity(), op.getLayerPicks());
                    StockLedger.Change change = change(MovementType.EXIT, op.getQuantity(), op.getNotes(), null);
                    return stockLedger.debit(ctx, refs.getT1(), refs.getT2(), change, op.getLots(), op.getLayerPicks());
                })
                .map(StockLedger.Result::movements));
    }

    @Override
    public Mono<List<StockMovement>> registerAdjustment(StockOperation op) {
        return inTransaction(ctx -> Mono.zip(
                        findActiveWarehouse(ctx.companyId(), op.getWarehouseId()),
                        findProduct(ctx.companyId(), op.getProductId()))
                .flatMap(refs -> {
                    Warehouse warehouse = refs.getT1();
                    Product product = refs.getT2();
                    requireWholeIfNeeded(product, op.getCountedQuantity());

                    // A product that controls expiry is counted lot by lot: the user says what each lot has.
                    if (op.getLotCounts() != null && !op.getLotCounts().isEmpty()) {
                        return adjustByLots(ctx, warehouse, product, op);
                    }

                    return stockRepositoryPort.findQuantity(warehouse.getId(), product.getId())
                            .defaultIfEmpty(BigDecimal.ZERO)
                            .flatMap(current -> {
                                BigDecimal delta = op.getCountedQuantity().subtract(current);
                                if (delta.signum() == 0) {
                                    return Mono.<StockLedger.Result>error(new BadRequestException(
                                            "La cantidad contada es igual al stock actual; no hay nada que ajustar"));
                                }
                                // More than the system says: it is stock coming in (a new lot if the product tracks
                                // expiry). Less: it leaves like an exit, earliest-expiring lots first.
                                return delta.signum() > 0
                                        ? stockLedger.credit(ctx, warehouse, product,
                                                change(MovementType.ADJUSTMENT, delta, op.getNotes(), null), lotOf(op), true)
                                        : stockLedger.debit(ctx, warehouse, product,
                                                change(MovementType.ADJUSTMENT, delta.negate(), op.getNotes(), null));
                            });
                })
                .map(StockLedger.Result::movements));
    }

    /**
     * Physical count of an expiry-tracked product, lot by lot. For each lot the system knows, the difference
     * between what was counted and what it had is applied to THAT lot (up or down); units counted without a date
     * adjust the stock that has no lot; lots the system didn't have are created. One ADJUSTMENT movement per change.
     */
    private Mono<StockLedger.Result> adjustByLots(StockContext ctx, Warehouse warehouse, Product product,
                                                  StockOperation op) {
        if (!Boolean.TRUE.equals(product.getTracksExpiry())) {
            return Mono.error(new BadRequestException(
                    "'" + product.getName() + "' no controla vencimiento: no tiene lotes que contar"));
        }

        List<LotCount> counts = op.getLotCounts();
        List<NewLot> news = op.getNewLots() == null ? List.of() : op.getNewLots();

        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LotCount count : counts) {
            if (count.lotId() != null && !seen.add(count.lotId())) {
                return Mono.error(new BadRequestException("Un mismo lote no puede repetirse"));
            }
            requireWholeIfNeeded(product, count.countedQuantity());
            total = total.add(count.countedQuantity());
        }
        for (NewLot lot : news) {
            requireWholeIfNeeded(product, lot.quantity());
            total = total.add(lot.quantity());
        }
        if (total.compareTo(op.getCountedQuantity()) != 0) {
            return Mono.error(new BadRequestException("La suma de los lotes contados ("
                    + total.stripTrailingZeros().toPlainString() + ") no coincide con la cantidad contada ("
                    + op.getCountedQuantity().stripTrailingZeros().toPlainString() + ")"));
        }

        return lotRepositoryPort.lockFefo(warehouse.getId(), product.getId())
                .collectList()
                .zipWith(stockRepositoryPort.findQuantity(warehouse.getId(), product.getId()).defaultIfEmpty(BigDecimal.ZERO))
                .flatMap(tuple -> {
                    List<StockLot> lots = tuple.getT1();
                    Map<Long, StockLot> byId = lots.stream().collect(Collectors.toMap(StockLot::getId, Function.identity()));
                    BigDecimal inLots = lots.stream().map(StockLot::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    // Units that never got an expiry date (stock from before the product controlled it).
                    BigDecimal withoutDate = tuple.getT2().subtract(inLots).max(BigDecimal.ZERO);

                    List<Mono<StockLedger.Result>> steps = new ArrayList<>();
                    for (LotCount count : counts) {
                        StockLot lot = count.lotId() == null ? null : byId.get(count.lotId());
                        if (count.lotId() != null && lot == null) {
                            return Mono.<StockLedger.Result>error(new BadRequestException(
                                    "El lote contado ya no existe o no tiene stock en este almacén; vuelve a contar"));
                        }
                        BigDecimal current = lot == null ? withoutDate : lot.getQuantity();
                        BigDecimal delta = count.countedQuantity().subtract(current);
                        if (delta.signum() == 0) {
                            continue;
                        }

                        if (delta.signum() > 0) {
                            StockLedger.LotSlice slice = lot == null
                                    ? null
                                    : new StockLedger.LotSlice(lot.getLotNumber(), lot.getExpiryDate(), delta);
                            steps.add(Mono.defer(() -> stockLedger.credit(ctx, warehouse, product,
                                    change(MovementType.ADJUSTMENT, delta, op.getNotes(), null), slice, false)));
                        } else {
                            BigDecimal out = delta.negate();
                            Long lotId = lot == null ? null : lot.getId();
                            steps.add(Mono.defer(() -> stockLedger.debit(ctx, warehouse, product,
                                    change(MovementType.ADJUSTMENT, out, op.getNotes(), null),
                                    List.of(new LotPick(lotId, out)))));
                        }
                    }
                    for (NewLot lot : news) {
                        steps.add(Mono.defer(() -> stockLedger.credit(ctx, warehouse, product,
                                change(MovementType.ADJUSTMENT, lot.quantity(), op.getNotes(), null),
                                new StockLedger.LotSlice(lot.lotNumber(), lot.expiryDate(), lot.quantity()), true)));
                    }

                    if (steps.isEmpty()) {
                        return Mono.<StockLedger.Result>error(new BadRequestException(
                                "Los conteos son iguales al stock actual; no hay nada que ajustar"));
                    }
                    return Flux.concat(steps)
                            .collectList()
                            .map(results -> new StockLedger.Result(
                                    results.stream().flatMap(result -> result.movements().stream()).toList(), List.of()));
                });
    }

    @Override
    public Mono<List<StockMovement>> registerTransfer(StockOperation op) {
        if (op.getWarehouseId() != null && op.getWarehouseId().equals(op.getToWarehouseId())) {
            return Mono.error(new BadRequestException("El almacén de origen y el de destino deben ser distintos"));
        }

        return inTransaction(ctx -> Mono.zip(
                        findActiveWarehouse(ctx.companyId(), op.getWarehouseId()),
                        findActiveWarehouse(ctx.companyId(), op.getToWarehouseId()),
                        findProduct(ctx.companyId(), op.getProductId()))
                .flatMap(refs -> {
                    Warehouse from = refs.getT1();
                    Warehouse to = refs.getT2();
                    Product product = refs.getT3();
                    requireWholeIfNeeded(product, op.getQuantity());
                    requireValidPicks(product, op.getQuantity(), op.getLots());
                    requireValidLayerPicks(product, op.getQuantity(), op.getLayerPicks());

                    String transferRef = UUID.randomUUID().toString();

                    // Origin first: if it lacks stock the whole transaction rolls back before anything is credited.
                    return stockLedger.debit(ctx, from, product,
                                    change(MovementType.TRANSFER_OUT, op.getQuantity(), op.getNotes(), transferRef),
                                    op.getLots(), op.getLayerPicks())
                            .flatMap(out -> Flux.fromIterable(out.slices())
                                    // Each lot that left arrives as the same lot (same expiry, same lot number).
                                    .concatMap(slice -> stockLedger.credit(ctx, to, product,
                                            // The units arrive with the cost they had where they left.
                                            change(MovementType.TRANSFER_IN, slice.quantity(), op.getNotes(), transferRef,
                                                    slice.unitCost()),
                                            slice, false))
                                    .collectList()
                                    .map(ins -> {
                                        List<StockMovement> all = new ArrayList<>(out.movements());
                                        ins.forEach(in -> all.addAll(in.movements()));
                                        return all;
                                    }));
                }));
    }

    private StockLedger.Change change(MovementType type, BigDecimal quantity, String notes, String transferRef) {
        return change(type, quantity, notes, transferRef, null);
    }

    /** {@code unitCost} null means the product's current cost. */
    private StockLedger.Change change(MovementType type, BigDecimal quantity, String notes, String transferRef,
                                      BigDecimal unitCost) {
        return new StockLedger.Change(type, quantity, notes, transferRef, null, unitCost, null);
    }

    /** The lot a user is entering, or null when the request carries none. */
    private StockLedger.LotSlice lotOf(StockOperation op) {
        if (op.getExpiryDate() == null) {
            return null;
        }
        return new StockLedger.LotSlice(op.getLotNumber(), op.getExpiryDate(), null);
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Mono<PageResponse<StockSummary>> findStock(int page, int size, String search, Long productId, Long warehouseId,
                                                      Boolean isActive, boolean lowStockOnly) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        // A user limited to some warehouses only sees (and adds up) the stock of those.
        return SecurityHelper.currentTenantUser().flatMap(user -> Mono.zip(
                        stockRepositoryPort.findSummaryPage(user.getCompanyId(), productId, warehouseId, user.getWarehouseIds(),
                                        isActive, pattern, lowStockOnly, safePage, safeSize)
                                .collectList(),
                        stockRepositoryPort.countSummary(user.getCompanyId(), productId, warehouseId, user.getWarehouseIds(),
                                isActive, pattern, lowStockOnly))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    @Override
    public Mono<InventoryValuation> getValuation() {
        return SecurityHelper.currentTenantUser().flatMap(user ->
                stockRepositoryPort.findValuation(user.getCompanyId(), user.getWarehouseIds())
                        .collectList()
                        .map(warehouses -> InventoryValuation.builder()
                                .warehouses(warehouses)
                                .totalValue(warehouses.stream()
                                        .map(WarehouseValuation::getValue)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                .build()));
    }

    @Override
    public Mono<PageResponse<StockLot>> findLots(int page, int size, Long productId, Long warehouseId, LotScope scope) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        LotScope safeScope = scope == null ? LotScope.ALL : scope;

        return SecurityHelper.currentTenantUser().flatMap(user ->
                settingsRepositoryPort.findByCompanyId(user.getCompanyId())
                        .defaultIfEmpty(CompanySettings.defaults(user.getCompanyId()))
                        .flatMap(settings -> {
                            int days = settings.getExpiryAlertDays() == null ? 30 : settings.getExpiryAlertDays();
                            return Mono.zip(
                                    lotRepositoryPort.findPage(user.getCompanyId(), productId, warehouseId,
                                                    user.getWarehouseIds(), safeScope, days, safePage, safeSize)
                                            .collectList(),
                                    lotRepositoryPort.count(user.getCompanyId(), productId, warehouseId,
                                            user.getWarehouseIds(), safeScope, days));
                        })
                        .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    @Override
    public Mono<List<CostLayer>> findCostLayers(Long productId, Long warehouseId) {
        return SecurityHelper.requireWarehouseAccess(warehouseId)
                .then(Mono.defer(() -> costLayerRepositoryPort.findAvailable(warehouseId, productId).collectList()));
    }

    @Override
    public Mono<PageResponse<StockMovement>> findMovements(int page, int size, Long productId, Long warehouseId,
                                                           MovementType type, Long purchaseOrderId) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);

        return SecurityHelper.currentTenantUser().flatMap(user -> Mono.zip(
                        movementRepositoryPort.findPage(user.getCompanyId(), productId, warehouseId, user.getWarehouseIds(),
                                type, purchaseOrderId, safePage, safeSize).collectList(),
                        movementRepositoryPort.count(user.getCompanyId(), productId, warehouseId, user.getWarehouseIds(),
                                type, purchaseOrderId))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    // ---------------------------------------------------------------- helpers

    /** Resolves the caller + company rule once, then runs the whole operation in a single transaction. */
    private <T> Mono<T> inTransaction(Function<StockContext, Mono<T>> operation) {
        return resolveContext().flatMap(ctx -> transactionPort.execute(operation.apply(ctx)));
    }

    private Mono<StockContext> resolveContext() {
        return SecurityHelper.currentUser()
                .switchIfEmpty(Mono.error(new UnauthorizedException("Debes iniciar sesión")))
                .flatMap(authUser -> {
                    Long companyId = authUser.getCompanyId();
                    if (companyId == null) {
                        return Mono.<StockContext>error(new ForbiddenException("Tu usuario no está asignado a una empresa"));
                    }
                    return settingsRepositoryPort.findByCompanyId(companyId)
                            .defaultIfEmpty(CompanySettings.defaults(companyId))
                            .map(settings -> new StockContext(
                                    companyId,
                                    authUser.getUserId(),
                                    Boolean.TRUE.equals(settings.getAllowNegativeStock())));
                });
    }

    /** Entries, exits, adjustments and both ends of a transfer all go through here, so this is where a user limited to some warehouses is stopped. */
    private Mono<Warehouse> findActiveWarehouse(Long companyId, Long warehouseId) {
        return SecurityHelper.requireWarehouseAccess(warehouseId)
                .then(Mono.defer(() -> warehouseRepositoryPort.findByIdAndCompanyId(warehouseId, companyId)))
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Warehouse", warehouseId)))
                .flatMap(warehouse -> Boolean.TRUE.equals(warehouse.getIsActive())
                        ? Mono.just(warehouse)
                        : Mono.<Warehouse>error(new BadRequestException(
                                "El almacén '" + warehouse.getName() + "' está inactivo")));
    }

    private Mono<Product> findProduct(Long companyId, Long productId) {
        return productRepositoryPort.findByIdAndCompanyId(productId, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Product", productId)));
    }

    /**
     * Lots chosen by the user: only for a product that controls expiry, each lot at most once, and adding up to
     * the quantity. (That each lot exists and has enough is checked against the locked lots, in the ledger.)
     */
    private void requireValidPicks(Product product, BigDecimal quantity, List<LotPick> picks) {
        if (picks == null || picks.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(product.getTracksExpiry())) {
            throw new BadRequestException("'" + product.getName() + "' no controla vencimiento: no tiene lotes para elegir");
        }
        BigDecimal total = BigDecimal.ZERO;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (LotPick pick : picks) {
            if (pick.lotId() != null && !seen.add(pick.lotId())) {
                throw new BadRequestException("Un mismo lote no puede repetirse");
            }
            // 1.5 + 0.5 adds up to a whole 2, but a product counted in units can't leave in halves.
            requireWholeIfNeeded(product, pick.quantity());
            total = total.add(pick.quantity());
        }
        if (total.compareTo(quantity) != 0) {
            throw new BadRequestException("La suma de los lotes elegidos ("
                    + total.stripTrailingZeros().toPlainString() + ") no coincide con la cantidad ("
                    + quantity.stripTrailingZeros().toPlainString() + ")");
        }
    }

    /**
     * Purchases chosen by the user (company cost method MANUAL): only for a product WITHOUT expiry control, since
     * those with expiry choose a lot (and take the oldest purchase of it); each purchase at most once, whole units
     * where the product needs them, and never more than the quantity that leaves.
     */
    private void requireValidLayerPicks(Product product, BigDecimal quantity, List<LayerPick> picks) {
        if (picks == null || picks.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(product.getTracksExpiry())) {
            throw new BadRequestException("'" + product.getName()
                    + "' controla vencimiento: se elige el lote, no la compra");
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (LayerPick pick : picks) {
            if (!seen.add(pick.layerId())) {
                throw new BadRequestException("Una misma compra no puede repetirse");
            }
            requireWholeIfNeeded(product, pick.quantity());
            total = total.add(pick.quantity());
        }
        if (total.compareTo(quantity) > 0) {
            throw new BadRequestException("Las compras elegidas ("
                    + total.stripTrailingZeros().toPlainString() + ") superan la cantidad que sale ("
                    + quantity.stripTrailingZeros().toPlainString() + ")");
        }
    }

    private void requireWholeIfNeeded(Product product, BigDecimal quantity) {
        if (product.getUnit().isWholeOnly() && quantity.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException(
                    "'" + product.getName() + "' se cuenta en unidades enteras; no se permiten cantidades con decimales");
        }
    }
}
