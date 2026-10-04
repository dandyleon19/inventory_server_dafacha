package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<User> findById(Long id);
    Mono<User> findByIdAndCompanyId(Long id, Long companyId);
    Mono<User> findByEmail(String email);
    /** Case-insensitive; {@code excludeId} skips that user (for edits). */
    Mono<Boolean> existsByEmail(String email, Long excludeId);
    Mono<Long> countAll();

    /** {@code searchPattern} is an already-escaped ILIKE pattern matched against name and email, or null. */
    Flux<User> findPage(Long companyId, Role role, Boolean isActive, String searchPattern, int page, int size);
    Mono<Long> count(Long companyId, Role role, Boolean isActive, String searchPattern);

    /** For each of the given users, the warehouses they are limited to (users without a restriction are absent). */
    Mono<Map<Long, List<Long>>> findWarehouseIds(Collection<Long> userIds);

    /** Replaces the user's warehouse restriction; an empty collection removes it. */
    Mono<Void> replaceWarehouses(Long userId, Collection<Long> warehouseIds);
}
