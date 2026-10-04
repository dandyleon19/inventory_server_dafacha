package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductUseCase {
    Mono<Product> create(Product product);
    Mono<Product> update(Long id, Product changes);
    Mono<Void> delete(Long id);
    Mono<Product> findById(Long id);
    Mono<PageResponse<Product>> findAll(int page, int size, String search, Long categoryId, Boolean isActive);
}
