package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.application.support.AuditDiff;
import com.dafacha.inventory.application.support.SearchSupport;
import com.dafacha.inventory.domain.exception.BadRequestException;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.ForbiddenException;
import com.dafacha.inventory.domain.exception.NotFoundException;
import com.dafacha.inventory.domain.model.AuditAction;
import com.dafacha.inventory.domain.model.AuditEntity;
import com.dafacha.inventory.domain.model.AuthUser;
import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.ports.in.UserUseCase;
import com.dafacha.inventory.domain.ports.out.RefreshTokenRepositoryPort;
import com.dafacha.inventory.domain.ports.out.UserAccessInvalidator;
import com.dafacha.inventory.domain.ports.out.UserRepositoryPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class UserServiceImpl implements UserUseCase {

    private static final String RESOURCE = "User";

    private final UserRepositoryPort userRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final UserAccessInvalidator accessInvalidator;
    private final AuditRecorder audit;

    // ---------------------------------------------------------------- create

    @Override
    public Mono<User> create(User user) {
        return SecurityHelper.currentTenantUser().flatMap(actor -> {
            normalize(user);
            if (user.getRole() == null) {
                return Mono.<User>error(new BadRequestException("El rol es obligatorio"));
            }
            if (user.getRole() == Role.SUPER_ADMIN) {
                return Mono.<User>error(new BadRequestException("El rol de super administrador no se puede asignar"));
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                return Mono.<User>error(new BadRequestException("La contraseña es obligatoria"));
            }

            Long companyId = actor.getCompanyId();
            // Admins are never limited to some warehouses, so there is nothing to store for them.
            List<Long> warehouseIds = user.getRole().isAdmin() ? List.of() : distinct(user.getWarehouseIds());

            return validateEmail(user.getEmail(), null)
                    .then(validateWarehouses(companyId, warehouseIds))
                    .then(Mono.defer(() -> {
                        user.setCompanyId(companyId);
                        user.setIsActive(true);
                        user.setPassword(passwordEncoder.encode(user.getPassword()));
                        return userRepositoryPort.save(user);
                    }))
                    .flatMap(saved -> userRepositoryPort.replaceWarehouses(saved.getId(), warehouseIds).thenReturn(saved))
                    .flatMap(saved -> {
                        saved.setWarehouseIds(warehouseIds);
                        return describeWarehouses(companyId, warehouseIds)
                                .flatMap(names -> audit.record(AuditAction.CREATE, AuditEntity.USER, saved.getId(),
                                        fullName(saved), AuditDiff.describe(snapshot(saved, names))))
                                .thenReturn(saved);
                    });
        });
    }

    // ---------------------------------------------------------------- update

    @Override
    public Mono<User> update(Long id, User changes) {
        return SecurityHelper.currentTenantUser().flatMap(actor ->
                find(id, actor.getCompanyId()).flatMap(existing -> {
                    normalize(changes);

                    Role newRole = changes.getRole() == null ? existing.getRole() : changes.getRole();
                    boolean newActive = changes.getIsActive() == null
                            ? Boolean.TRUE.equals(existing.getIsActive())
                            : changes.getIsActive();

                    Mono<Void> guard = guardUpdate(actor, existing, newRole, newActive);
                    return guard.then(Mono.defer(() -> applyUpdate(actor, existing, changes, newRole, newActive)));
                }));
    }

    /** Rules that protect the account owner and the caller from locking themselves (or everyone) out. */
    private Mono<Void> guardUpdate(AuthUser actor, User existing, Role newRole, boolean newActive) {
        if (existing.getRole() == Role.SUPER_ADMIN && !SecurityHelper.isSuperAdmin(actor)) {
            return Mono.error(new ForbiddenException("Solo un super administrador puede modificar a un super administrador"));
        }
        if (newRole == Role.SUPER_ADMIN && existing.getRole() != Role.SUPER_ADMIN) {
            return Mono.error(new BadRequestException("El rol de super administrador no se puede asignar"));
        }
        if (existing.getRole() == Role.SUPER_ADMIN && newRole != Role.SUPER_ADMIN) {
            return Mono.error(new BadRequestException("No se puede cambiar el rol de un super administrador"));
        }
        if (existing.getRole() == Role.SUPER_ADMIN && !newActive) {
            return Mono.error(new BadRequestException("No se puede desactivar a un super administrador"));
        }
        boolean self = actor.getUserId().equals(existing.getId());
        if (self && (newRole != existing.getRole() || !newActive)) {
            return Mono.error(new BadRequestException("No puedes cambiar tu propio rol ni desactivar tu propia cuenta"));
        }
        return Mono.empty();
    }

    private Mono<User> applyUpdate(AuthUser actor, User existing, User changes, Role newRole, boolean newActive) {
        Long id = existing.getId();
        Long companyId = actor.getCompanyId();
        List<Long> newWarehouseIds = newRole.isAdmin() ? List.of() : distinct(changes.getWarehouseIds());

        return currentWarehouseIds(id)
                .flatMap(oldIds -> describeWarehouses(companyId, oldIds).flatMap(oldNames -> {
                    Map<String, Object> before = snapshot(withWarehouses(existing, oldIds), oldNames);

                    existing.setFirstName(changes.getFirstName());
                    existing.setLastName(changes.getLastName());
                    existing.setEmail(changes.getEmail());
                    existing.setRole(newRole);
                    existing.setIsActive(newActive);
                    existing.setUpdatedAt(LocalDateTime.now());

                    return validateEmail(existing.getEmail(), id)
                            .then(validateWarehouses(companyId, newWarehouseIds))
                            .then(Mono.defer(() -> userRepositoryPort.save(existing)))
                            .flatMap(saved -> userRepositoryPort.replaceWarehouses(id, newWarehouseIds).thenReturn(saved))
                            .flatMap(saved -> {
                                saved.setWarehouseIds(newWarehouseIds);
                                // The role / status / warehouses apply to the user's very next request.
                                accessInvalidator.evict(id);
                                Mono<Void> endSessions = newActive
                                        ? Mono.<Void>empty()
                                        : refreshTokenRepositoryPort.revokeAllByUserId(id);
                                return endSessions
                                        .then(describeWarehouses(companyId, newWarehouseIds))
                                        .flatMap(newNames -> audit.record(AuditAction.UPDATE, AuditEntity.USER, id,
                                                fullName(saved), AuditDiff.between(before, snapshot(saved, newNames))))
                                        .thenReturn(saved);
                            });
                }));
    }

    // ---------------------------------------------------------------- password

    @Override
    public Mono<Void> resetPassword(Long id, String newPassword) {
        return SecurityHelper.currentTenantUser().flatMap(actor ->
                find(id, actor.getCompanyId()).flatMap(existing -> {
                    if (existing.getRole() == Role.SUPER_ADMIN && !SecurityHelper.isSuperAdmin(actor)) {
                        return Mono.<Void>error(new ForbiddenException("Solo un super administrador puede modificar a un super administrador"));
                    }
                    existing.setPassword(passwordEncoder.encode(newPassword));
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userRepositoryPort.save(existing)
                            // The old password may be in someone else's hands: every open session ends.
                            .then(Mono.defer(() -> refreshTokenRepositoryPort.revokeAllByUserId(id)))
                            .then(Mono.defer(() -> {
                                accessInvalidator.evict(id);
                                return audit.record(AuditAction.PASSWORD_RESET, AuditEntity.USER, id,
                                        fullName(existing), null);
                            }));
                }));
    }

    // ---------------------------------------------------------------- queries

    @Override
    public Mono<User> findById(Long id) {
        return SecurityHelper.currentCompanyId().flatMap(companyId ->
                find(id, companyId).flatMap(user -> currentWarehouseIds(id).map(ids -> withWarehouses(user, ids))));
    }

    @Override
    public Mono<PageResponse<User>> findAll(int page, int size, String search, Role role, Boolean isActive) {
        int safePage = SearchSupport.safePage(page);
        int safeSize = SearchSupport.safeSize(size);
        String pattern = SearchSupport.likePattern(search);

        return SecurityHelper.currentCompanyId().flatMap(companyId -> Mono.zip(
                        userRepositoryPort.findPage(companyId, role, isActive, pattern, safePage, safeSize)
                                .collectList()
                                .flatMap(this::withWarehouseIds),
                        userRepositoryPort.count(companyId, role, isActive, pattern))
                .map(tuple -> PageResponse.of(tuple.getT1(), safePage, safeSize, tuple.getT2())));
    }

    // ---------------------------------------------------------------- helpers

    private Mono<User> find(Long id, Long companyId) {
        return userRepositoryPort.findByIdAndCompanyId(id, companyId)
                .switchIfEmpty(Mono.error(NotFoundException.forResource(RESOURCE, id)));
    }

    /** One lookup for the whole page instead of one per user. */
    private Mono<List<User>> withWarehouseIds(List<User> users) {
        return userRepositoryPort.findWarehouseIds(users.stream().map(User::getId).toList())
                .map(byUser -> {
                    users.forEach(user -> user.setWarehouseIds(byUser.getOrDefault(user.getId(), List.of())));
                    return users;
                });
    }

    private Mono<List<Long>> currentWarehouseIds(Long userId) {
        return userRepositoryPort.findWarehouseIds(List.of(userId))
                .map(byUser -> byUser.getOrDefault(userId, List.<Long>of()));
    }

    private static User withWarehouses(User user, List<Long> warehouseIds) {
        user.setWarehouseIds(warehouseIds);
        return user;
    }

    private Mono<Void> validateEmail(String email, Long excludeId) {
        return userRepositoryPort.existsByEmail(email, excludeId)
                .filter(Boolean::booleanValue)
                .flatMap(exists -> Mono.<Void>error(
                        new ConflictException("Ya existe un usuario con el correo '" + email + "'")));
    }

    private Mono<Void> validateWarehouses(Long companyId, List<Long> warehouseIds) {
        return Flux.fromIterable(warehouseIds)
                .concatMap(warehouseId -> warehouseRepositoryPort.findByIdAndCompanyId(warehouseId, companyId)
                        .switchIfEmpty(Mono.error(NotFoundException.forResource("Warehouse", warehouseId))))
                .then();
    }

    /** "Todos" when there is no restriction, otherwise the names, for the audit trail. */
    private Mono<String> describeWarehouses(Long companyId, List<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Mono.just("Todos");
        }
        return Flux.fromIterable(warehouseIds)
                .concatMap(warehouseId -> warehouseRepositoryPort.findByIdAndCompanyId(warehouseId, companyId)
                        .map(Warehouse::getName)
                        .defaultIfEmpty("#" + warehouseId))
                .collectList()
                .map(names -> String.join(", ", names));
    }

    private Map<String, Object> snapshot(User user, String warehouseNames) {
        Map<String, Object> snapshot = AuditDiff.snapshot();
        snapshot.put("Nombre", user.getFirstName());
        snapshot.put("Apellido", user.getLastName());
        snapshot.put("Correo", user.getEmail());
        snapshot.put("Rol", roleLabel(user.getRole()));
        snapshot.put("Activo", user.getIsActive());
        snapshot.put("Almacenes", warehouseNames);
        return snapshot;
    }

    private static String roleLabel(Role role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case SUPER_ADMIN -> "Superadministrador";
            case ADMIN_USER -> "Administrador";
            case WAREHOUSE_USER -> "Encargado de almacén";
            case PURCHASING_USER -> "Compras";
            case VIEWER -> "Solo lectura";
        };
    }

    private static String fullName(User user) {
        return ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
    }

    private static List<Long> distinct(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void normalize(User user) {
        user.setFirstName(user.getFirstName() == null ? null : user.getFirstName().trim());
        user.setLastName(user.getLastName() == null ? null : user.getLastName().trim());
        user.setEmail(user.getEmail() == null ? null : user.getEmail().trim());
    }
}
