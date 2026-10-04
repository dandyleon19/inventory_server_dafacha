package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.ProductCategoryEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductCategoryRepository extends R2dbcRepository<ProductCategoryEntity, Long> {

    Mono<ProductCategoryEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT id, company_id, name, description, created_at, updated_at
            FROM product_categories
            WHERE company_id = :companyId
              AND (CAST(:search AS TEXT) IS NULL OR name ILIKE :search)
            ORDER BY name ASC, id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ProductCategoryEntity> findPage(Long companyId, String search, int limit, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM product_categories
            WHERE company_id = :companyId
              AND (CAST(:search AS TEXT) IS NULL OR name ILIKE :search)
            """)
    Mono<Long> countFiltered(Long companyId, String search);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM product_categories
                WHERE company_id = :companyId
                  AND LOWER(name) = LOWER(:name)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByName(Long companyId, String name, Long excludeId);
}
