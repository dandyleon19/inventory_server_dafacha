package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.StockSummary;
import com.dafacha.inventory.domain.model.WarehouseValuation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public interface StockRepositoryPort {

    /**
     * Atomically adds {@code delta} (may be negative) to the stock of a product in a warehouse, creating the
     * row if needed, and returns the new quantity. Returns EMPTY, changing nothing, when the result would be
     * negative and {@code allowNegative} is false. The check and the change happen in one atomic conditional
     * update, so concurrent operations can't oversell the same stock.
     */
    Mono<BigDecimal> applyDelta(Long companyId, Long warehouseId, Long productId, BigDecimal delta, boolean allowNegative);

    /**
     * Recomputes the product's cost as the weighted average of the stock it has (all warehouses) and the
     * {@code quantity} that is arriving at {@code unitCost}, and returns the new cost. With no stock (zero or
     * negative) the new cost is simply {@code unitCost}. Call it BEFORE adding the arriving quantity to the stock.
     */
    Mono<BigDecimal> updateWeightedAverageCost(Long companyId, Long productId, BigDecimal quantity, BigDecimal unitCost);

    /** Whether the product has stock (a non-zero quantity) in any warehouse. */
    Mono<Boolean> existsStock(Long productId);

    /** Current quantity; empty when the product has never had stock in that warehouse. */
    Mono<BigDecimal> findQuantity(Long warehouseId, Long productId);

    /**
     * Products with their stock (all warehouses, or just {@code warehouseId} when given). {@code allowedWarehouseIds}
     * limits the quantities to the warehouses a restricted user may see; null means all of them.
     */
    Flux<StockSummary> findSummaryPage(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                                       Boolean isActive, String searchPattern, boolean lowStockOnly, int page, int size);

    Mono<Long> countSummary(Long companyId, Long productId, Long warehouseId, List<Long> allowedWarehouseIds,
                            Boolean isActive, String searchPattern, boolean lowStockOnly);

    /** One row per warehouse of the company (only the allowed ones when restricted), with stock x current cost. */
    Flux<WarehouseValuation> findValuation(Long companyId, List<Long> allowedWarehouseIds);
}
