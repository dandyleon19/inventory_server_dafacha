package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.WarehouseEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WarehouseRepository extends R2dbcRepository<WarehouseEntity, Long> {

    Mono<WarehouseEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT id, company_id, name, address, is_active, created_at, updated_at
            FROM warehouses
            WHERE company_id = :companyId
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (CAST(:search AS TEXT) IS NULL OR name ILIKE :search OR address ILIKE :search)
            ORDER BY name ASC, id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<WarehouseEntity> findPage(Long companyId, Boolean isActive, String search, int limit, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM warehouses
            WHERE company_id = :companyId
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (CAST(:search AS TEXT) IS NULL OR name ILIKE :search OR address ILIKE :search)
            """)
    Mono<Long> countFiltered(Long companyId, Boolean isActive, String search);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM warehouses
                WHERE company_id = :companyId
                  AND LOWER(name) = LOWER(:name)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);
}
