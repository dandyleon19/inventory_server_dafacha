package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.domain.exception.BadRequestException;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.ForbiddenException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.exception.UnauthorizedException;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderLine;
import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import com.dafacha.inventory.domain.model.ReceiptItem;
import com.dafacha.inventory.domain.ports.in.PurchaseOrderUseCase;
import com.dafacha.inventory.domain.ports.out.DocumentSequencePort;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.domain.ports.out.PurchaseOrderRepositoryPort;
import com.dafacha.inventory.domain.ports.out.StockRepositoryPort;
import com.dafacha.inventory.domain.ports.out.SupplierRepositoryPort;
import com.dafacha.inventory.domain.ports.out.TransactionPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderUseCase {

    private static final String SEQUENCE = "PURCHASE_ORDER";

    private final PurchaseOrderRepositoryPort orderRepositoryPort;
    private final SupplierRepositoryPort supplierRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final StockRepositoryPort stockRepositoryPort;
    private final StockLedger stockLedger;
    private final DocumentSequencePort documentSequencePort;
    private final TransactionPort transactionPort;
    private final AuditRecorder audit;

    private record Context(Long companyId, Long userId) {
    }

    // ---------------------------------------------------------------- public operations (each one leaves an audit entry)

    @Override
    public Mono<PurchaseOrder> create(PurchaseOrder draft) {
        return validateExpectedDate(null, draft.getExpectedDate())
                .then(Mono.defer(() -> doCreate(draft)))
                .flatMap(created -> record(AuditAction.CREATE, created, AuditDiff.describe(snapshot(created))));
    }

    @Override
    public Mono<PurchaseOrder> update(Long id, PurchaseOrder changes) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> find(id, companyId).flatMap(existing -> {
            Map<String, Object> before = snapshot(existing);
            return validateExpectedDate(existing.getExpectedDate(), changes.getExpectedDate())
                    .then(Mono.defer(() -> doUpdate(id, changes)))
                    .flatMap(updated -> record(AuditAction.UPDATE, updated, AuditDiff.between(before, snapshot(updated))));
        }));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> find(id, companyId).flatMap(existing ->
                doDelete(id).then(Mono.defer(() -> record(AuditAction.DELETE, existing, AuditDiff.describe(snapshot(existing)))
                        .then()))));
    }

    @Override
    public Mono<PurchaseOrder> markOrdered(Long id) {
        return doMarkOrdered(id).flatMap(ordered -> record(AuditAction.ORDER, ordered, null));
    }

    @Override
    public Mono<PurchaseOrder> cancel(Long id) {
        return doCancel(id).flatMap(cancelled -> record(AuditAction.CANCEL, cancelled, null));
    }

    @Override
    public Mono<PurchaseOrder> receive(Long id, LocalDate receivedDate, List<ReceiptItem> items) {
        LocalDate day = receivedDate == null ? LocalDate.now() : receivedDate;
        return doReceive(id, day, items)
                .flatMap(order -> record(AuditAction.RECEIVE, order, describeReceipt(order, day, items)));
    }

    /**
     * The expected delivery date can't be before today. A draft that already carries an older date keeps it
     * (editing its lines isn't blocked); only choosing a different past date is refused.
     */
    private Mono<Void> validateExpectedDate(LocalDate previous, LocalDate expected) {
        if (expected == null || !expected.isBefore(LocalDate.now()) || expected.equals(previous)) {
            return Mono.empty();
        }
        return Mono.error(new BadRequestException("La fecha de entrega no puede ser anterior a hoy"));
    }

    private Mono<PurchaseOrder> record(AuditAction action, PurchaseOrder order, String details) {
        return audit.record(action, AuditEntity.PURCHASE_ORDER, order.getId(), order.getNumber(), details)
                .thenReturn(order);
    }

    private static Map<String, Object> snapshot(PurchaseOrder order) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Proveedor", order.getSupplierName());
        snapshot.put("Almacén", order.getWarehouseName());
        snapshot.put("Entrega esperada", order.getExpectedDate());
        snapshot.put("Notas", order.getNotes());
        snapshot.put("Total", order.getTotal());
        snapshot.put("Productos", order.getLines() == null ? null : order.getLines().stream()
                .map(line -> line.getProductName() + " × " + AuditDiff.format(line.getQuantity())
                        + " a " + AuditDiff.format(line.getUnitCost()))
                .collect(Collectors.joining("; ")));
        return snapshot;
    }

    /** What arrived in this receipt: one line per item, with its lot and expiry when it has them. */
    private static String describeReceipt(PurchaseOrder order, LocalDate day, List<ReceiptItem> items) {
        Map<Long, PurchaseOrderLine> linesById = order.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
        String header = day.equals(LocalDate.now()) ? "" : "Fecha de recepción: " + day + "\n";
        return header + items.stream().map(item -> {
            PurchaseOrderLine line = linesById.get(item.getLineId());
            String name = line == null ? "#" + item.getLineId() : line.getProductName();
            String lot = item.getExpiryDate() == null ? ""
                    : " (" + (item.getLotNumber() == null || item.getLotNumber().isBlank() ? "" : "lote " + item.getLotNumber() + ", ")
                    + "vence " + item.getExpiryDate() + ")";
            return name + ": +" + AuditDiff.format(item.getQuantity()) + lot;
        }).collect(Collectors.joining("\n"));
    }

    // ---------------------------------------------------------------- draft lifecycle

    private Mono<PurchaseOrder> doCreate(PurchaseOrder draft) {
        return inTransaction(ctx -> validateOrder(ctx.companyId(), draft)
                .then(Mono.defer(() -> documentSequencePort.next(ctx.companyId(), SEQUENCE)))
                .flatMap(sequence -> {
                    draft.setCompanyId(ctx.companyId());
                    draft.setNumber(String.format("OC-%06d", sequence));
                    draft.setStatus(PurchaseOrderStatus.DRAFT);
                    draft.setCreatedBy(ctx.userId());
                    draft.setNotes(SearchSupport.blankToNull(draft.getNotes()));
                    draft.setTotal(PurchaseOrder.totalOf(draft.getLines()));
                    return orderRepositoryPort.saveHeader(draft);
                })
                .flatMap(saved -> orderRepositoryPort.replaceLines(saved.getId(), draft.getLines())
                        .then(Mono.defer(() -> orderRepositoryPort.findDetail(saved.getId(), ctx.companyId())))));
    }

    private Mono<PurchaseOrder> doUpdate(Long id, PurchaseOrder changes) {
        return inTransaction(ctx -> find(id, ctx.companyId()).flatMap(existing -> {
            requireStatus(existing, "Solo se puede editar una orden en borrador", PurchaseOrderStatus.DRAFT);

            return validateOrder(ctx.companyId(), changes).then(Mono.defer(() -> {
                existing.setSupplierId(changes.getSupplierId());
                existing.setWarehouseId(changes.getWarehouseId());
                existing.setExpectedDate(changes.getExpectedDate());
                existing.setNotes(SearchSupport.blankToNull(changes.getNotes()));
                existing.setTotal(PurchaseOrder.totalOf(changes.getLines()));
                existing.setUpdatedAt(LocalDateTime.now());

                return orderRepositoryPort.saveHeader(existing)
                        .then(Mono.defer(() -> orderRepositoryPort.replaceLines(id, changes.getLines())))
                        .then(Mono.defer(() -> orderRepositoryPort.findDetail(id, ctx.companyId())));
            }));
        }));
    }

    private Mono<Void> doDelete(Long id) {
        return inTransaction(ctx -> find(id, ctx.companyId()).flatMap(existing -> {
            requireStatus(existing, "Solo se puede eliminar una orden en borrador", PurchaseOrderStatus.DRAFT);
            return orderRepositoryPort.deleteById(id);
        }));
    }

    private Mono<PurchaseOrder> doMarkOrdered(Long id) {
        return inTransaction(ctx -> find(id, ctx.companyId()).flatMap(existing -> {
            requireStatus(existing, "Solo una orden en borrador se puede marcar como pedida", PurchaseOrderStatus.DRAFT);

            existing.setStatus(PurchaseOrderStatus.ORDERED);
            existing.setOrderedAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            return orderRepositoryPort.saveHeader(existing)
                    .then(Mono.defer(() -> orderRepositoryPort.findDetail(id, ctx.companyId())));
        }));
    }

    private Mono<PurchaseOrder> doCancel(Long id) {
        return inTransaction(ctx -> find(id, ctx.companyId()).flatMap(existing -> {
            requireStatus(existing, "Una orden recibida o ya cancelada no se puede cancelar",
                    PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

            existing.setStatus(PurchaseOrderStatus.CANCELLED);
            existing.setCancelledAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            return orderRepositoryPort.saveHeader(existing)
                    .then(Mono.defer(() -> orderRepositoryPort.findDetail(id, ctx.companyId())));
        }));
    }

    // ---------------------------------------------------------------- receiving goods

    private Mono<PurchaseOrder> doReceive(Long id, LocalDate receivedDate, List<ReceiptItem> items) {
        return inTransaction(ctx -> find(id, ctx.companyId()).flatMap(order -> {
            requireStatus(order, "Solo una orden pedida o parcialmente recibida puede recibir mercadería",
                    PurchaseOrderStatus.ORDERED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

            Map<Long, PurchaseOrderLine> linesById = order.getLines().stream()
                    .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
            Map<Long, BigDecimal> receivedNow = validateReceipt(items, linesById);
            validateReceivedDate(order, receivedDate);
            // Today means "right now"; an earlier day is recorded at midday of that day.
            LocalDateTime occurredAt = receivedDate.equals(LocalDate.now())
                    ? LocalDateTime.now()
                    : receivedDate.atTime(LocalTime.NOON);

            return requireActiveWarehouse(ctx.companyId(), order.getWarehouseId())
                    .then(Flux.fromIterable(items)
                            .concatMap(item -> receiveLine(ctx, order, linesById.get(item.getLineId()), item, occurredAt))
                            .then())
                    .then(Mono.defer(() -> {
                        boolean complete = order.getLines().stream().allMatch(line ->
                                line.getPendingQuantity()
                                        .subtract(receivedNow.getOrDefault(line.getId(), BigDecimal.ZERO))
                                        .signum() == 0);

                        order.setStatus(complete ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepositoryPort.saveHeader(order);
                    }))
                    .then(Mono.defer(() -> orderRepositoryPort.findDetail(id, ctx.companyId())));
        }));
    }

    /**
     * One received line, in this order: (1) recompute the product cost from the stock it has BEFORE this arrival,
     * (2) add the stock through the ledger (which also creates the lot of an expiry-tracked product and logs the
     * PURCHASE movement with the purchase cost), (3) record it on the order line.
     */
    private Mono<Void> receiveLine(Context ctx, PurchaseOrder order, PurchaseOrderLine line, ReceiptItem item,
                                   LocalDateTime occurredAt) {
        BigDecimal quantity = item.getQuantity();

        // Strictly one step after another, and each step must produce something: if any of them came back empty
        // the old code skipped the stock and the movement but still marked the line as received, leaving an order
        // that says "received" with nothing in the stock or the log.
        return stockRepositoryPort
                .updateWeightedAverageCost(ctx.companyId(), line.getProductId(), quantity, line.getUnitCost())
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Product", line.getProductId())))
                .then(Mono.defer(() -> warehouseRepositoryPort.findByIdAndCompanyId(order.getWarehouseId(), ctx.companyId())
                        .switchIfEmpty(Mono.error(NotFoundException.forResource("Warehouse", order.getWarehouseId())))))
                .flatMap(warehouse -> productRepositoryPort.findByIdAndCompanyId(line.getProductId(), ctx.companyId())
                        .switchIfEmpty(Mono.error(NotFoundException.forResource("Product", line.getProductId())))
                        .flatMap(product -> {
                            // The movement carries the purchase cost (what this batch actually cost), not the
                            // product's average cost, which the update above has just recalculated.
                            StockLedger.Change change = new StockLedger.Change(
                                    MovementType.PURCHASE, quantity, "Recepción " + order.getNumber(), null,
                                    order.getId(), line.getUnitCost(), occurredAt);
                            StockLedger.LotSlice lot = item.getExpiryDate() == null
                                    ? null
                                    : new StockLedger.LotSlice(item.getLotNumber(), item.getExpiryDate(), quantity);

                            return stockLedger.credit(
                                    new StockContext(ctx.companyId(), ctx.userId(), false),
                                    warehouse, product, change, lot, true);
                        }))
                .switchIfEmpty(Mono.error(new ConflictException(
                        "La recepción de '" + line.getProductName() + "' no generó movimiento de stock; no se guardó nada")))
                .then(Mono.defer(() -> orderRepositoryPort.addReceivedQuantity(line.getId(), quantity)));
    }

    /** The goods can't have arrived in the future, nor before the order was placed. */
    private void validateReceivedDate(PurchaseOrder order, LocalDate receivedDate) {
        if (receivedDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha de recepción no puede ser futura");
        }
        if (order.getOrderedAt() != null && receivedDate.isBefore(order.getOrderedAt().toLocalDate())) {
            throw new BadRequestException("La fecha de recepción no puede ser anterior al día del pedido");
        }
    }

    /** Checks every received item against its line and returns the quantity received per line id. */
    private Map<Long, BigDecimal> validateReceipt(List<ReceiptItem> items, Map<Long, PurchaseOrderLine> linesById) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Indica al menos un producto para recibir");
        }

        Map<Long, BigDecimal> receivedNow = new HashMap<>();
        for (ReceiptItem item : items) {
            PurchaseOrderLine line = linesById.get(item.getLineId());
            if (line == null) {
                throw NotFoundException.forResource("Purchase order line", item.getLineId());
            }
            if (receivedNow.containsKey(item.getLineId())) {
                throw new BadRequestException("Una línea no puede repetirse en la misma recepción");
            }
            if (item.getQuantity().compareTo(line.getPendingQuantity()) > 0) {
                throw new BadRequestException("No puedes recibir más de lo pendiente de '" + line.getProductName()
                        + "' (pendiente: " + line.getPendingQuantity().stripTrailingZeros().toPlainString() + ")");
            }
            requireWholeIfNeeded(line, item.getQuantity());
            receivedNow.put(item.getLineId(), item.getQuantity());
        }
        return receivedNow;
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Mono<PurchaseOrder> findById(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId -> find(id, companyId));
    }

    @Override
    public Mono<PageResponse<PurchaseOrder>> findAll(int page, int size, String search, PurchaseOrderStatus status,
                                                     Long supplierId) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        // A user limited to some warehouses only sees the orders that arrive at those.
        return SecurityHelper.currentTenantUser().flatMap(user -> Mono.zip(
                        orderRepositoryPort.findPage(user.getCompanyId(), user.getWarehouseIds(), status, supplierId,
                                pattern, safePage, safeSize).collectList(),
                        orderRepositoryPort.count(user.getCompanyId(), user.getWarehouseIds(), status, supplierId, pattern))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    // ---------------------------------------------------------------- validation helpers

    private Mono<PurchaseOrder> find(Long id, Long companyId) {
        return orderRepositoryPort.findDetail(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Purchase order", id)))
                .flatMap(order -> SecurityHelper.currentTenantUser().flatMap(user ->
                        user.canAccessWarehouse(order.getWarehouseId())
                                ? Mono.just(order)
                                : Mono.<PurchaseOrder>error(NotFoundException.forResource("Purchase order", id))));
    }

    private static String statusLabel(PurchaseOrderStatus status) {
        return switch (status) {
            case DRAFT -> "Borrador";
            case ORDERED -> "Pedida";
            case PARTIALLY_RECEIVED -> "Parcialmente recibida";
            case RECEIVED -> "Recibida";
            case CANCELLED -> "Cancelada";
        };
    }

    private void requireStatus(PurchaseOrder order, String message, PurchaseOrderStatus... allowed) {
        for (PurchaseOrderStatus status : allowed) {
            if (order.getStatus() == status) {
                return;
            }
        }
        throw new ConflictException(message + " (estado actual: " + statusLabel(order.getStatus()) + ")");
    }

    /**
     * Header and lines of a draft: supplier and warehouse usable, products belong to the company. A product may
     * appear on several lines (one at the package price, another for extra units at a different price).
     */
    private Mono<Void> validateOrder(Long companyId, PurchaseOrder order) {
        List<PurchaseOrderLine> lines = order.getLines();
        if (lines == null || lines.isEmpty()) {
            return Mono.error(new BadRequestException("La orden de compra necesita al menos una línea"));
        }

        Mono<Void> supplierCheck = supplierRepositoryPort.findByIdAndCompanyId(order.getSupplierId(), companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Supplier", order.getSupplierId())))
                .flatMap(supplier -> Boolean.TRUE.equals(supplier.getIsActive())
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(new BadRequestException("El proveedor '" + supplier.getName() + "' está inactivo")));

        Mono<Void> productsCheck = Flux.fromIterable(lines)
                .concatMap(line -> productRepositoryPort.findByIdAndCompanyId(line.getProductId(), companyId)
                        .switchIfEmpty(Mono.error(NotFoundException.forResource("Product", line.getProductId())))
                        .doOnNext(product -> {
                            if (product.getUnit().isWholeOnly() && line.getQuantity().stripTrailingZeros().scale() > 0) {
                                throw new BadRequestException("'" + product.getName()
                                        + "' se cuenta en unidades enteras; no se permiten cantidades con decimales");
                            }
                        }))
                .then();

        return supplierCheck
                .then(requireActiveWarehouse(companyId, order.getWarehouseId()))
                .then(productsCheck);
    }

    private Mono<Void> requireActiveWarehouse(Long companyId, Long warehouseId) {
        return SecurityHelper.requireWarehouseAccess(warehouseId)
                .then(Mono.defer(() -> warehouseRepositoryPort.findByIdAndCompanyId(warehouseId, companyId)))
                .switchIfEmpty(Mono.error(NotFoundException.forResource("Warehouse", warehouseId)))
                .flatMap(warehouse -> Boolean.TRUE.equals(warehouse.getIsActive())
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(new BadRequestException("El almacén '" + warehouse.getName() + "' está inactivo")));
    }

    private void requireWholeIfNeeded(PurchaseOrderLine line, BigDecimal quantity) {
        if (line.getUnit() != null && line.getUnit().isWholeOnly() && quantity.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException(
                    "'" + line.getProductName() + "' se cuenta en unidades enteras; no se permiten cantidades con decimales");
        }
    }

    // ---------------------------------------------------------------- transaction + caller

    private <T> Mono<T> inTransaction(Function<Context, Mono<T>> operation) {
        return resolveContext().flatMap(ctx -> transactionPort.execute(operation.apply(ctx)));
    }

    private Mono<Context> resolveContext() {
        return SecurityHelper.currentUser()
                .switchIfEmpty(Mono.error(new UnauthorizedException("Debes iniciar sesión")))
                .flatMap(authUser -> authUser.getCompanyId() == null
                        ? Mono.<Context>error(new ForbiddenException("Tu usuario no está asignado a una empresa"))
                        : Mono.just(new Context(authUser.getCompanyId(), authUser.getUserId())));
    }
}
