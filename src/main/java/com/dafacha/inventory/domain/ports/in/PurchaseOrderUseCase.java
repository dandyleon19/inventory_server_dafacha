package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.PurchaseOrder;
import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import com.dafacha.inventory.domain.model.ReceiptItem;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Lifecycle: DRAFT -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED, or CANCELLED.
 * Only a DRAFT can be edited or deleted; goods can be received once it is ORDERED.
 */
public interface PurchaseOrderUseCase {

    /** Creates a DRAFT. Uses supplierId, warehouseId, expectedDate, notes and lines (productId, quantity, unitCost). */
    Mono<PurchaseOrder> create(PurchaseOrder draft);

    /** Replaces the header and the lines of a DRAFT. */
    Mono<PurchaseOrder> update(Long id, PurchaseOrder changes);

    /** Deletes a DRAFT. */
    Mono<Void> delete(Long id);

    /** DRAFT -> ORDERED: the order is considered sent to the supplier. */
    Mono<PurchaseOrder> markOrdered(Long id);

    /**
     * Receives goods: for each item adds stock to the order's warehouse (a PURCHASE movement) and updates the
     * product cost by weighted average. Everything happens in one transaction. Returns the updated order.
     */
    Mono<PurchaseOrder> receive(Long id, LocalDate receivedDate, List<ReceiptItem> items);

    /** Cancels what is still pending. Stock already received is kept. */
    Mono<PurchaseOrder> cancel(Long id);

    Mono<PurchaseOrder> findById(Long id);

    Mono<PageResponse<PurchaseOrder>> findAll(int page, int size, String search, PurchaseOrderStatus status, Long supplierId);
}
