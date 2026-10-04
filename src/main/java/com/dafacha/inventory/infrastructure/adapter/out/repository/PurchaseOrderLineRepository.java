package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.PurchaseOrderLineEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PurchaseOrderLineRepository extends R2dbcRepository<PurchaseOrderLineEntity, Long> {

    @Modifying
    @Query("DELETE FROM purchase_order_lines WHERE purchase_order_id = :purchaseOrderId")
    Mono<Integer> deleteByOrder(Long purchaseOrderId);

    /** The CHECK (received_quantity <= quantity) on the table is the last line of defence against over-receiving. */
    @Modifying
    @Query("UPDATE purchase_order_lines SET received_quantity = received_quantity + :quantity WHERE id = :id")
    Mono<Integer> addReceived(Long id, BigDecimal quantity);
}
