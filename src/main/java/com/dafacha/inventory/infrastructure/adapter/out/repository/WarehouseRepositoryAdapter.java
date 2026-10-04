package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class WarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    private final WarehouseRepository repository;
    private final WarehouseMapper mapper;

    @Override
    public Mono<Warehouse> save(Warehouse warehouse) {
        return repository.save(mapper.toEntity(warehouse)).map(mapper::toDomain);
    }

    @Override
    public Mono<Warehouse> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Flux<Warehouse> findPage(Long companyId, Boolean isActive, String searchPattern, int page, int size) {
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
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
