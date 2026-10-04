package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.ports.out.CompanyRepositoryPort;
import com.dafacha.inventory.infrastructure.adapter.out.mapper.CompanyMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public Mono<Company> save(Company company) {
        return companyRepository.save(companyMapper.toEntity(company))
                .map(companyMapper::toDomain);
    }

    @Override
    public Mono<Company> findById(Long id) {
        return companyRepository.findById(id)
                .map(companyMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByRucNumber(String rucNumber, Long excludeId) {
        return companyRepository.existsByRucNumber(rucNumber, excludeId);
    }
}
