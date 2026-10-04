package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {
    Mono<Product> save(Product product);
    Mono<Product> findByIdAndCompanyId(Long id, Long companyId);
    /** {@code searchPattern} is an already-escaped ILIKE pattern, or null for no filter. */
    Flux<Product> findPage(Long companyId, Long categoryId, Boolean isActive, String searchPattern, int page, int size);
    Mono<Long> count(Long companyId, Long categoryId, Boolean isActive, String searchPattern);
    Mono<Boolean> existsBySku(Long companyId, String sku, Long excludeId);
    Mono<Boolean> existsByBarcode(Long companyId, String barcode, Long excludeId);
    Mono<Long> countByCategoryId(Long companyId, Long categoryId);
    Mono<Void> deleteById(Long id);
}
