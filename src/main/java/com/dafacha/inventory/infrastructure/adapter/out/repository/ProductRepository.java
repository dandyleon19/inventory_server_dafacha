package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.ProductEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepository extends R2dbcRepository<ProductEntity, Long> {

    Mono<ProductEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT id, company_id, category_id, name, description, sku, barcode, unit,
                   cost_price, sale_price, min_stock, tracks_expiry, is_active, created_at, updated_at
            FROM products
            WHERE company_id = :companyId
              AND (CAST(:categoryId AS BIGINT) IS NULL OR category_id = :categoryId)
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR name ILIKE :search
                  OR sku ILIKE :search
                  OR barcode ILIKE :search
              )
            ORDER BY name ASC, id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<ProductEntity> findPage(Long companyId, Long categoryId, Boolean isActive, String search, int limit, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM products
            WHERE company_id = :companyId
              AND (CAST(:categoryId AS BIGINT) IS NULL OR category_id = :categoryId)
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR name ILIKE :search
                  OR sku ILIKE :search
                  OR barcode ILIKE :search
              )
            """)
    Mono<Long> countFiltered(Long companyId, Long categoryId, Boolean isActive, String search);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM products
                WHERE company_id = :companyId
                  AND LOWER(sku) = LOWER(:sku)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsBySku(Long companyId, String sku, Long excludeId);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM products
                WHERE company_id = :companyId
                  AND barcode = :barcode
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByBarcode(Long companyId, String barcode, Long excludeId);

    @Query("SELECT COUNT(*) FROM products WHERE company_id = :companyId AND category_id = :categoryId")
    Mono<Long> countByCategory(Long companyId, Long categoryId);
}
