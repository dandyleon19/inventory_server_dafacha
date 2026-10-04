package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.StockLot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockLotRepositoryPort {

    /** Adds to the lot with that (warehouse, product, expiry, lot number), creating it if it doesn't exist. */
    Mono<Void> addToLot(Long companyId, Long warehouseId, Long productId, String lotNumber, LocalDate expiryDate,
                        BigDecimal quantity);

    /**
     * The lots of a product in a warehouse that still have stock, earliest expiry first, locked for update so
     * a concurrent exit can't take the same units. Must run inside the operation's transaction.
     */
    Flux<StockLot> lockFefo(Long warehouseId, Long productId);

    /** Whether the product still has any lot with stock, in any warehouse. */
    Mono<Boolean> existsWithStock(Long productId);

    /** Takes {@code quantity} from a lot; a lot that reaches zero is deleted. */
    Mono<Void> consume(Long lotId, BigDecimal quantity);

    /**
     * Lots with stock, soonest expiry first, with names filled in. {@code days} is the "expiring" window.
     * {@code allowedWarehouseIds} limits the result to a restricted user's warehouses; null means all.
     */
    Flux<StockLot> findPage(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                            LotScope scope, int days, int page, int size);

    Mono<Long> count(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                     LotScope scope, int days);
}
