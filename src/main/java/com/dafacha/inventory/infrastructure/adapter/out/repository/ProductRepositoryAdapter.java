package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.Product;
import com.dafacha.inventory.domain.ports.out.ProductRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public Mono<Product> save(Product product) {
        return repository.save(mapper.toEntity(product)).map(mapper::toDomain);
    }

    @Override
    public Mono<Product> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findPage(Long companyId, Long categoryId, Boolean isActive, String searchPattern, int page, int size) {
        return repository.findPage(companyId, categoryId, isActive, searchPattern, size, (long) page * size)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> count(Long companyId, Long categoryId, Boolean isActive, String searchPattern) {
        return repository.countFiltered(companyId, categoryId, isActive, searchPattern);
    }

    @Override
    public Mono<Boolean> existsBySku(Long companyId, String sku, Long excludeId) {
        return repository.existsBySku(companyId, sku, excludeId);
    }

    @Override
    public Mono<Boolean> existsByBarcode(Long companyId, String barcode, Long excludeId) {
        return repository.existsByBarcode(companyId, barcode, excludeId);
    }

    @Override
    public Mono<Long> countByCategoryId(Long companyId, Long categoryId) {
        return repository.countByCategory(companyId, categoryId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
