package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.ProductCategory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ProductCategoryRepositoryPort {
    Mono<ProductCategory> save(ProductCategory category);
    Mono<ProductCategory> findByIdAndCompanyId(Long id, Long companyId);
    Flux<ProductCategory> findAllByIds(Collection<Long> ids);
    /** {@code searchPattern} is an already-escaped ILIKE pattern, or null for no filter. */
    Flux<ProductCategory> findPage(Long companyId, String searchPattern, int page, int size);
    Mono<Long> count(Long companyId, String searchPattern);
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);
    Mono<Void> deleteById(Long id);
}
