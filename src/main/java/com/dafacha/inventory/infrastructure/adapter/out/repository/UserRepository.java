package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<UserEntity> findByEmail(String email);

    Mono<UserEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            SELECT id, first_name, last_name, email, password, is_active, role, company_id, created_at, updated_at
            FROM users
            WHERE company_id = :companyId
              AND (CAST(:role AS TEXT) IS NULL OR role = :role)
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR first_name ILIKE :search
                  OR last_name ILIKE :search
                  OR email ILIKE :search
                  OR CONCAT(first_name, ' ', last_name) ILIKE :search
              )
            ORDER BY first_name ASC, last_name ASC, id ASC
            LIMIT :limit OFFSET :offset
            """)
    Flux<UserEntity> findPage(Long companyId, String role, Boolean isActive, String search, int limit, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM users
            WHERE company_id = :companyId
              AND (CAST(:role AS TEXT) IS NULL OR role = :role)
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR is_active = :isActive)
              AND (
                  CAST(:search AS TEXT) IS NULL
                  OR first_name ILIKE :search
                  OR last_name ILIKE :search
                  OR email ILIKE :search
                  OR CONCAT(first_name, ' ', last_name) ILIKE :search
              )
            """)
    Mono<Long> countFiltered(Long companyId, String role, Boolean isActive, String search);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM users
                WHERE LOWER(email) = LOWER(:email)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
            )
            """)
    Mono<Boolean> existsByEmail(String email, Long excludeId);
}
