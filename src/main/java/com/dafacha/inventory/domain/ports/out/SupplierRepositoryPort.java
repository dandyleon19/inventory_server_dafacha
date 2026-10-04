package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupplierRepositoryPort {
    Mono<Supplier> save(Supplier supplier);
    Mono<Supplier> findByIdAndCompanyId(Long id, Long companyId);
    /** {@code searchPattern} is an already-escaped ILIKE pattern, or null for no filter. */
    Flux<Supplier> findPage(Long companyId, Boolean isActive, String searchPattern, int page, int size);
    Mono<Long> count(Long companyId, Boolean isActive, String searchPattern);
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);
    Mono<Boolean> existsByRucNumber(Long companyId, String rucNumber, Long excludeId);
    Mono<Void> deleteById(Long id);
}
