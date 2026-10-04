package com.dafacha.inventory.infrastructure.adapter.in;

import com.dafacha.inventory.application.dto.request.ProductCategoryRequest;
import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.dto.response.ProductCategoryResponse;
import com.dafacha.inventory.application.mapper.RequestDtoMapper;
import com.dafacha.inventory.domain.ports.in.ProductCategoryUseCase;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductCategoryMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryUseCase productCategoryUseCase;
    private final RequestDtoMapper requestDtoMapper;
    private final ProductCategoryMapper productCategoryMapper;

    @GetMapping
    public Mono<ResponseEntity<PageResponse<ProductCategoryResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        return productCategoryUseCase.findAll(page, size, search)
                .map(result -> result.map(productCategoryMapper::toResponse))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProductCategoryResponse>> findById(@PathVariable Long id) {
        return productCategoryUseCase.findById(id)
                .map(productCategoryMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<ProductCategoryResponse>> create(@Valid @RequestBody ProductCategoryRequest request) {
        return productCategoryUseCase.create(requestDtoMapper.toProductCategory(request))
                .map(productCategoryMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProductCategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryRequest request
    ) {
        return productCategoryUseCase.update(id, requestDtoMapper.toProductCategory(request))
                .map(productCategoryMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return productCategoryUseCase.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
