package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.StockMovement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface StockMovementRepositoryPort {
    Mono<StockMovement> save(StockMovement movement);
    /**
     * Newest first, with warehouse / product / user names filled in. {@code allowedWarehouseIds} limits the result
     * to a restricted user's warehouses; null means all.
     */
    Flux<StockMovement> findPage(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                                 MovementType type, Long purchaseOrderId, int page, int size);
    Mono<Long> count(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                     MovementType type, Long purchaseOrderId);
    Mono<Boolean> existsByWarehouseId(Long warehouseId);
    Mono<Boolean> existsByProductId(Long productId);
}
