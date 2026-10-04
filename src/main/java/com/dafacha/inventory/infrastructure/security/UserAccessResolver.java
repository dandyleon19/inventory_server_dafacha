package com.dafacha.inventory.infrastructure.security;

import com.dafacha.inventory.domain.model.AuthUser;
import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.ports.out.UserAccessInvalidator;
import com.dafacha.inventory.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns the user id in a valid token into the caller as it is NOW: role, active flag, company and warehouses are
 * read from the database, not from the token. The access token lasts a day, so trusting its claims would let a
 * deactivated user, or a demoted admin, keep working until it expires.
 *
 * Results are cached for a few seconds so a busy page doesn't turn into a query per request; whoever changes a
 * user calls {@link #evict(Long)} so the change applies immediately in this instance.
 */
@RequiredArgsConstructor
public class UserAccessResolver implements UserAccessInvalidator {

    private static final long TTL_MILLIS = 10_000;

    /** A null user is a cached "not allowed" (inactive or gone), so a rejected token doesn't hit the database either. */
    private record Entry(AuthUser user, long expiresAt) {
    }

    private final UserRepositoryPort userRepositoryPort;
    private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();

    /** Empty when the user doesn't exist or is inactive: the request is then treated as unauthenticated. */
    public Mono<AuthUser> resolve(Long userId) {
        Entry cached = cache.get(userId);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached.user() == null ? Mono.empty() : Mono.just(cached.user());
        }

        return userRepositoryPort.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .flatMap(user -> warehousesOf(user).map(warehouseIds -> toAuthUser(user, warehouseIds)))
                .doOnNext(authUser -> remember(userId, authUser))
                .switchIfEmpty(Mono.<AuthUser>fromRunnable(() -> remember(userId, null)));
    }

    @Override
    public void evict(Long userId) {
        cache.remove(userId);
    }

    private void remember(Long userId, AuthUser authUser) {
        cache.put(userId, new Entry(authUser, System.currentTimeMillis() + TTL_MILLIS));
    }

    /**
     * The warehouses stored for the user. Admins are never limited, so theirs are ignored. An empty result means
     * "no restriction" (see {@link #toAuthUser}).
     */
    private Mono<List<Long>> warehousesOf(User user) {
        if (user.getRole() == null || user.getRole().isAdmin()) {
            return Mono.just(List.<Long>of());
        }
        return userRepositoryPort.findWarehouseIds(List.of(user.getId()))
                .map(byUser -> byUser.getOrDefault(user.getId(), List.<Long>of()));
    }

    /** No rows means no restriction (null); rows mean the user can only use those warehouses. */
    private AuthUser toAuthUser(User user, List<Long> warehouseIds) {
        Role role = user.getRole();
        return AuthUser.builder()
                .userId(user.getId())
                .companyId(user.getCompanyId())
                .role(role == null ? null : role.name())
                .userName((safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                .warehouseIds(warehouseIds.isEmpty() ? null : List.copyOf(warehouseIds))
                .build();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
