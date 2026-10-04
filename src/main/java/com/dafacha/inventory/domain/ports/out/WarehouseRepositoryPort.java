package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.Warehouse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WarehouseRepositoryPort {
    Mono<Warehouse> save(Warehouse warehouse);
    Mono<Warehouse> findByIdAndCompanyId(Long id, Long companyId);
    /** {@code searchPattern} is an already-escaped ILIKE pattern, or null for no filter. */
    Flux<Warehouse> findPage(Long companyId, Boolean isActive, String searchPattern, int page, int size);
    Mono<Long> count(Long companyId, Boolean isActive, String searchPattern);
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);
    Mono<Void> deleteById(Long id);
}
