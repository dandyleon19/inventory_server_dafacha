package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.StockAdjustmentRequest;
import com.dafacha.inventory.application.dto.request.StockMovementRequest;
import com.dafacha.inventory.application.dto.request.StockTransferRequest;
import com.dafacha.inventory.application.dto.response.CostLayerResponse;
import com.dafacha.inventory.application.dto.response.InventoryValuationResponse;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.StockLotResponse;
import com.dafacha.inventory.application.dto.response.StockMovementResponse;
import com.dafacha.inventory.application.dto.response.StockSummaryResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.model.LotScope;
import com.dafacha.inventory.domain.model.MovementType;
import com.dafacha.inventory.domain.model.StockMovement;
import com.dafacha.inventory.domain.ports.in.StockUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMapper;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.StockMovementMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final StockMapper stockMapper;
    private final StockMovementMapper stockMovementMapper;

    /** Products with their stock: all warehouses, or only {@code warehouseId} when given. */
    @GetMapping
    public Mono<ResponseEntity<PageResponse<StockSummaryResponse>>> findStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "false") boolean lowStockOnly
    ) {
        return stockUseCase.findStock(page, size, search, productId, warehouseId, isActive, lowStockOnly)
                .map(result -> result.map(stockMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/valuation")
    public Mono<ResponseEntity<InventoryValuationResponse>> getValuation() {
        return stockUseCase.getValuation()
                .map(stockMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    /** Lots with stock, soonest expiry first. scope: ALL, EXPIRED, EXPIRING (company alert window) or ATTENTION (both). */
    @GetMapping("/lots")
    public Mono<ResponseEntity<PageResponse<StockLotResponse>>> findLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "ALL") LotScope scope
    ) {
        return stockUseCase.findLots(page, size, productId, warehouseId, scope)
                .map(result -> result.map(stockMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    /** The purchases of a product still in a warehouse (what each cost, how much is left), oldest first. */
    @GetMapping("/cost-layers")
    public Mono<ResponseEntity<List<CostLayerResponse>>> findCostLayers(
            @RequestParam Long productId,
            @RequestParam Long warehouseId
    ) {
        return stockUseCase.findCostLayers(productId, warehouseId)
                .map(layers -> layers.stream().map(CostLayerResponse::from).toList())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/movements")
    public Mono<ResponseEntity<PageResponse<StockMovementResponse>>> findMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) Long purchaseOrderId
    ) {
        return stockUseCase.findMovements(page, size, productId, warehouseId, type, purchaseOrderId)
                .map(result -> result.map(stockMovementMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/entries")
    public Mono<ResponseEntity<List<StockMovementResponse>>> registerEntry(@Valid @RequestBody StockMovementRequest request) {
        return created(stockUseCase.registerEntry(requestDtoMapper.toStockOperation(request)));
    }

    @PostMapping("/exits")
    public Mono<ResponseEntity<List<StockMovementResponse>>> registerExit(@Valid @RequestBody StockMovementRequest request) {
        return created(stockUseCase.registerExit(requestDtoMapper.toStockOperation(request)));
    }

    @PostMapping("/adjustments")
    public Mono<ResponseEntity<List<StockMovementResponse>>> registerAdjustment(@Valid @RequestBody StockAdjustmentRequest request) {
        return created(stockUseCase.registerAdjustment(requestDtoMapper.toStockOperation(request)));
    }

    @PostMapping("/transfers")
    public Mono<ResponseEntity<List<StockMovementResponse>>> registerTransfer(@Valid @RequestBody StockTransferRequest request) {
        return created(stockUseCase.registerTransfer(requestDtoMapper.toStockOperation(request)));
    }

    private Mono<ResponseEntity<List<StockMovementResponse>>> created(Mono<List<StockMovement>> movements) {
        return movements
                .map(list -> list.stream().map(stockMovementMapper::toResponse).toList())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }
}
