package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.Supplier;
import com.dafacha.inventory.domain.ports.out.SupplierRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SupplierRepositoryAdapter implements SupplierRepositoryPort {

    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    @Override
    public Mono<Supplier> save(Supplier supplier) {
        return repository.save(mapper.toEntity(supplier)).map(mapper::toDomain);
    }

    @Override
    public Mono<Supplier> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Flux<Supplier> findPage(Long companyId, Boolean isActive, String searchPattern, int page, int size) {
        return repository.findPage(companyId, isActive, searchPattern, size, (long) page * size)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> count(Long companyId, Boolean isActive, String searchPattern) {
        return repository.countFiltered(companyId, isActive, searchPattern);
    }

    @Override
    public Mono<Boolean> existsByName(Long companyId, String name, Long excludeId) {
        return repository.existsByName(companyId, name, excludeId);
    }

    @Override
    public Mono<Boolean> existsByRucNumber(Long companyId, String rucNumber, Long excludeId) {
        return repository.existsByRucNumber(companyId, rucNumber, excludeId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
