package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.SupplierRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.SupplierResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.ports.in.SupplierUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.SupplierMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierUseCase supplierUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final SupplierMapper supplierMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<SupplierResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive
    ) {
        return supplierUseCase.findAll(page, size, search, isActive)
                .map(result -> result.map(supplierMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<SupplierResponse>> findById(@PathVariable Long id) {
        return supplierUseCase.findById(id)
                .map(supplierMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<SupplierResponse>> create(@Valid @RequestBody SupplierRequest request) {
        return supplierUseCase.create(requestDtoMapper.toSupplier(request))
                .map(supplierMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<SupplierResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request
    ) {
        return supplierUseCase.update(id, requestDtoMapper.toSupplier(request))
                .map(supplierMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return supplierUseCase.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
