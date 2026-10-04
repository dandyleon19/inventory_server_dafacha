package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.CostLayer;
import com.dafacha.inventory.domain.model.InventoryValuation;
import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.StockLot;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.model.StockOperation;
import com.dafacha.inventory.domain.model.StockSummary;
import reactor.core.publisher.Mono;

import java.util.List;

public interface StockUseCase {

    /** Stock in. Uses warehouseId, productId, quantity, notes. */
    Mono<List<StockMovement>> registerEntry(StockOperation operation);

    /** Stock out. Uses warehouseId, productId, quantity, notes. Rejected if it would go below zero (unless allowed). */
    Mono<List<StockMovement>> registerExit(StockOperation operation);

    /** Physical count: sets the stock to countedQuantity and logs the difference. Uses warehouseId, productId, countedQuantity, notes. */
    Mono<List<StockMovement>> registerAdjustment(StockOperation operation);

    /** Moves stock between two warehouses atomically; returns the OUT leg then the IN leg. */
    Mono<List<StockMovement>> registerTransfer(StockOperation operation);

    Mono<PageResponse<StockSummary>> findStock(int page, int size, String search, Long productId, Long warehouseId,
                                               Boolean isActive, boolean lowStockOnly);

    Mono<InventoryValuation> getValuation();

    /** Lots with stock, soonest expiry first. EXPIRING / ATTENTION use the company's alert window. */
    Mono<PageResponse<StockLot>> findLots(int page, int size, Long productId, Long warehouseId, LotScope scope);

    /** The purchases (cost layers) a product still has in a warehouse, oldest first. For choosing which one an exit comes from. */
    Mono<List<CostLayer>> findCostLayers(Long productId, Long warehouseId);

    Mono<PageResponse<StockMovement>> findMovements(int page, int size, Long productId, Long warehouseId,
                                                    MovementType type, Long purchaseOrderId);
}
