package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.CompanyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface CompanyRepository extends R2dbcRepository<CompanyEntity, Long> {

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM companies
                WHERE ruc_number = :rucNumber
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByRucNumber(String rucNumber, Long excludeId);
}
