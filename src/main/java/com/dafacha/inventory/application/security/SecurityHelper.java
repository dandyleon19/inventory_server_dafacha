package com.dafacha.inventory.application.security;

import com.dafacha.inventory.domain.exception.ForbiddenException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.exception.UnauthorizedException;
import com.dafacha.inventory.domain.model.AuthUser;
import com.dafacha.inventory.domain.model.Role;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public final class SecurityHelper {

    private SecurityHelper() {
    }

    public static Mono<AuthUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (AuthUser) ctx.getAuthentication().getPrincipal());
    }

    /**
     * The caller's company id. All catalog/stock data is scoped to it, so every use case starts from here;
     * a user without a company can't read or write tenant data.
     */
    public static Mono<Long> currentCompanyId() {
        return currentTenantUser().map(AuthUser::getCompanyId);
    }

    /** The authenticated caller, who must belong to a company. */
    public static Mono<AuthUser> currentTenantUser() {
        return currentUser()
                .switchIfEmpty(Mono.error(new UnauthorizedException("Debes iniciar sesión")))
                .flatMap(authUser -> authUser.getCompanyId() == null
                        ? Mono.<AuthUser>error(new ForbiddenException("Tu usuario no está asignado a una empresa"))
                        : Mono.just(authUser));
    }

    /** Fails with Forbidden unless the caller may use every one of the given warehouses. */
    public static Mono<Void> requireWarehouseAccess(Long... warehouseIds) {
        return currentTenantUser().flatMap(authUser -> {
            for (Long warehouseId : warehouseIds) {
                if (warehouseId != null && !authUser.canAccessWarehouse(warehouseId)) {
                    return Mono.<Void>error(new ForbiddenException("No tienes acceso a este almacén"));
                }
            }
            return Mono.<Void>empty();
        });
    }

    public static boolean isSuperAdmin(AuthUser authUser) {
        return Role.SUPER_ADMIN.name().equals(authUser.getRole());
    }

    public static boolean isAdmin(AuthUser authUser) {
        return Role.SUPER_ADMIN.name().equals(authUser.getRole()) || Role.ADMIN_USER.name().equals(authUser.getRole());
    }

    /**
     * Tenant guard: passes the entity through only if it belongs to the caller's company.
     * Cross-tenant access reports NotFound (not Forbidden) so ids can't be probed.
     */
    public static <T> Mono<T> requireSameCompany(T entity, Long entityCompanyId, AuthUser authUser, String resource, Object id) {
        if (isSuperAdmin(authUser)) {
            return Mono.just(entity);
        }
        if (entityCompanyId == null || !entityCompanyId.equals(authUser.getCompanyId())) {
            return Mono.error(NotFoundException.forResource(resource, id));
        }
        return Mono.just(entity);
    }

    public static Mono<Void> requireCompanyAccess(Long companyId, AuthUser authUser, String resource, Object id) {
        if (isSuperAdmin(authUser)) {
            return Mono.empty();
        }
        if (companyId == null || !companyId.equals(authUser.getCompanyId())) {
            return Mono.error(NotFoundException.forResource(resource, id));
        }
        return Mono.empty();
    }
}
