package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.PurchaseOrderEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface PurchaseOrderRepository extends R2dbcRepository<PurchaseOrderEntity, Long> {

    @Query("SELECT EXISTS (SELECT 1 FROM purchase_orders WHERE supplier_id = :supplierId)")
    Mono<Boolean> existsBySupplier(Long supplierId);
}
