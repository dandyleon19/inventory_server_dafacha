package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.ProductRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.ProductResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.ports.in.ProductUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final ProductMapper productMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<ProductResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive
    ) {
        return productUseCase.findAll(page, size, search, categoryId, isActive)
                .map(result -> result.map(productMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> findById(@PathVariable Long id) {
        return productUseCase.findById(id)
                .map(productMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return productUseCase.create(requestDtoMapper.toProduct(request))
                .map(productMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productUseCase.update(id, requestDtoMapper.toProduct(request))
                .map(productMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return productUseCase.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
