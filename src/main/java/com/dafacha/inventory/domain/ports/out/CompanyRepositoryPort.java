package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.Company;
import reactor.core.publisher.Mono;

public interface CompanyRepositoryPort {
    Mono<Company> save(Company company);
    Mono<Company> findById(Long id);
    Mono<Boolean> existsByRucNumber(String rucNumber, Long excludeId);
}
