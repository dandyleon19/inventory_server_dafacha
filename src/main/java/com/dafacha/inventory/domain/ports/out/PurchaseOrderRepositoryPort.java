package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderLine;
import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseOrderRepositoryPort {

    /** Inserts (no id) or updates the order row only; lines are handled by {@link #replaceLines}. */
    Mono<PurchaseOrder> saveHeader(PurchaseOrder order);

    /** Deletes the order's lines and inserts the given ones. */
    Mono<Void> replaceLines(Long purchaseOrderId, List<PurchaseOrderLine> lines);

    /** The order with its lines and the supplier / warehouse / product names filled in. */
    Mono<PurchaseOrder> findDetail(Long id, Long companyId);

    /**
     * Orders without lines but with {@code lineCount}, newest first. {@code searchPattern} matches number or supplier.
     * {@code allowedWarehouseIds} keeps only the orders for a restricted user's warehouses; null means all. The same
     * parameter limits the queries below.
     */
    Flux<PurchaseOrder> findPage(Long companyId, List<Long> allowedWarehouseIds, PurchaseOrderStatus status,
                                 Long supplierId, String searchPattern, int page, int size);

    Mono<Long> count(Long companyId, List<Long> allowedWarehouseIds, PurchaseOrderStatus status, Long supplierId,
                     String searchPattern);

    /**
     * Open orders (ordered or partially received) expected within {@code days} days, overdue ones included,
     * soonest first. Header data only, no lines.
     */
    Flux<PurchaseOrder> findArriving(Long companyId, List<Long> allowedWarehouseIds, int days, int limit);

    Mono<Long> countArriving(Long companyId, List<Long> allowedWarehouseIds, int days);

    /** Open orders whose expected date has already passed. */
    Mono<Long> countOverdue(Long companyId, List<Long> allowedWarehouseIds);

    /** Adds to the received quantity of one line. */
    Mono<Void> addReceivedQuantity(Long lineId, BigDecimal quantity);

    Mono<Boolean> existsBySupplierId(Long supplierId);

    Mono<Void> deleteById(Long id);
}
