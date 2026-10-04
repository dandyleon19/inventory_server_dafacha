package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.ProductCategory;
import com.dafacha.inventory.domain.ports.out.ProductCategoryRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductCategoryMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@RequiredArgsConstructor
public class ProductCategoryRepositoryAdapter implements ProductCategoryRepositoryPort {

    private final ProductCategoryRepository repository;
    private final ProductCategoryMapper mapper;

    @Override
    public Mono<ProductCategory> save(ProductCategory category) {
        return repository.save(mapper.toEntity(category)).map(mapper::toDomain);
    }

    @Override
    public Mono<ProductCategory> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Flux<ProductCategory> findAllByIds(Collection<Long> ids) {
        return repository.findAllById(ids).map(mapper::toDomain);
    }

    @Override
    public Flux<ProductCategory> findPage(Long companyId, String searchPattern, int page, int size) {
        return repository.findPage(companyId, searchPattern, size, (long) page * size).map(mapper::toDomain);
    }

    @Override
    public Mono<Long> count(Long companyId, String searchPattern) {
        return repository.countFiltered(companyId, searchPattern);
    }

    @Override
    public Mono<Boolean> existsByName(Long companyId, String name, Long excludeId) {
        return repository.existsByName(companyId, name, excludeId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
