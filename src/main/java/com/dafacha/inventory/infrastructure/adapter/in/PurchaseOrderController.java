package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.PurchaseOrderRequest;
import com.dafacha.inventory.application.dto.request.ReceivePurchaseOrderRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.PurchaseOrderResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import com.dafacha.inventory.domain.ports.in.PurchaseOrderUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.PurchaseOrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderUseCase purchaseOrderUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<PurchaseOrderResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId
    ) {
        return purchaseOrderUseCase.findAll(page, size, search, status, supplierId)
                .map(result -> result.map(purchaseOrderMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PurchaseOrderResponse>> findById(@PathVariable Long id) {
        return purchaseOrderUseCase.findById(id)
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<PurchaseOrderResponse>> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderUseCase.create(requestDtoMapper.toPurchaseOrder(request))
                .map(purchaseOrderMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<PurchaseOrderResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request
    ) {
        return purchaseOrderUseCase.update(id, requestDtoMapper.toPurchaseOrder(request))
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return purchaseOrderUseCase.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/order")
    public Mono<ResponseEntity<PurchaseOrderResponse>> markOrdered(@PathVariable Long id) {
        return purchaseOrderUseCase.markOrdered(id)
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/receive")
    public Mono<ResponseEntity<PurchaseOrderResponse>> receive(
            @PathVariable Long id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request
    ) {
        return purchaseOrderUseCase.receive(id, request.getReceivedDate(), requestDtoMapper.toReceiptItems(request))
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/cancel")
    public Mono<ResponseEntity<PurchaseOrderResponse>> cancel(@PathVariable Long id) {
        return purchaseOrderUseCase.cancel(id)
                .map(purchaseOrderMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
