package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.WarehouseRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.WarehouseResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.ports.in.WarehouseUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.WarehouseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseUseCase warehouseUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final WarehouseMapper warehouseMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<WarehouseResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive
    ) {
        return warehouseUseCase.findAll(page, size, search, isActive)
                .map(result -> result.map(warehouseMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<WarehouseResponse>> findById(@PathVariable Long id) {
        return warehouseUseCase.findById(id)
                .map(warehouseMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<WarehouseResponse>> create(@Valid @RequestBody WarehouseRequest request) {
        return warehouseUseCase.create(requestDtoMapper.toWarehouse(request))
                .map(warehouseMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<WarehouseResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request
    ) {
        return warehouseUseCase.update(id, requestDtoMapper.toWarehouse(request))
                .map(warehouseMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return warehouseUseCase.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
