package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.StockMovementEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface StockMovementRepository extends R2dbcRepository<StockMovementEntity, Long> {

    @Query("SELECT EXISTS (SELECT 1 FROM stock_movements WHERE warehouse_id = :warehouseId)")
    Mono<Boolean> existsByWarehouse(Long warehouseId);

    @Query("SELECT EXISTS (SELECT 1 FROM stock_movements WHERE product_id = :productId)")
    Mono<Boolean> existsByProduct(Long productId);
}
