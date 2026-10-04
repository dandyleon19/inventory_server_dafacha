package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.ProductCategory;
import reactor.core.publisher.Mono;

public interface ProductCategoryUseCase {
    Mono<ProductCategory> create(ProductCategory category);
    Mono<ProductCategory> update(Long id, ProductCategory changes);
    Mono<Void> delete(Long id);
    Mono<ProductCategory> findById(Long id);
    Mono<PageResponse<ProductCategory>> findAll(int page, int size, String search);
}
