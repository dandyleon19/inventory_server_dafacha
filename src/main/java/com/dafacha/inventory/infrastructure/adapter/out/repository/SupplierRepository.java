package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.SupplierEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupplierRepository extends R2dbcRepository<SupplierEntity, Long> {

    Mono<SupplierEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT id, company_id, name, ruc_number, contact_name, phone, email, address, notes,
                   is_active, created_at, updated_at
            FROM suppliers
            WHERE company_id = :companyId
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR name ILIKE :search
                  OR ruc_number ILIKE :search
                  OR contact_name ILIKE :search
              )
            ORDER BY name ASC, id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<SupplierEntity> findPage(Long companyId, Boolean isActive, String search, int limit, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM suppliers
            WHERE company_id = :companyId
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR name ILIKE :search
                  OR ruc_number ILIKE :search
                  OR contact_name ILIKE :search
              )
            """)
    Mono<Long> countFiltered(Long companyId, Boolean isActive, String search);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM suppliers
                WHERE company_id = :companyId
                  AND LOWER(name) = LOWER(:name)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM suppliers
                WHERE company_id = :companyId
                  AND ruc_number = :rucNumber
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByRucNumber(Long companyId, String rucNumber, Long excludeId);
}
