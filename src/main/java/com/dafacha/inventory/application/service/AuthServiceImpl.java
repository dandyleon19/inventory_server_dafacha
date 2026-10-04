package com.dafacha.inventory.application.service;

import com.dafacha.inventory.application.dto.response.AuthResponse;
import com.dafacha.inventory.application.security.SecurityHelper;
import com.dafacha.inventory.config.properties.JwtProperties;
import com.dafacha.inventory.domain.exception.ConflictException;
import com.dafacha.inventory.domain.exception.UnauthorizedException;
import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.model.RefreshToken;
import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import com.dafacha.inventory.domain.model.Warehouse;
import com.dafacha.inventory.domain.ports.in.AuthUseCase;
import com.dafacha.inventory.domain.ports.out.CompanyRepositoryPort;
import com.dafacha.inventory.domain.ports.out.RefreshTokenRepositoryPort;
import com.dafacha.inventory.domain.ports.out.UserRepositoryPort;
import com.dafacha.inventory.domain.ports.out.WarehouseRepositoryPort;
import com.dafacha.inventory.infrastructure.security.JwtService;
import com.dafacha.inventory.infrastructure.security.RefreshTokenGenerator;
import com.dafacha.inventory.infrastructure.security.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditRecorder auditRecorder;

    @Override
    public Mono<AuthResponse> login(String email, String password) {
        return userRepositoryPort.findByEmail(email)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Correo o contraseña incorrectos")))
                .flatMap(user -> {
                    boolean matches = passwordEncoder.matches(password, user.getPassword());
                    if (!matches || !Boolean.TRUE.equals(user.getIsActive())) {
                        return Mono.error(new UnauthorizedException("Correo o contraseña incorrectos"));
                    }

                    return issueAuthResponse(user)
                            .flatMap(response -> auditRecorder.recordLogin(user).thenReturn(response));
                });
    }

    @Override
    public Mono<AuthResponse> bootstrap(User admin, Company company) {
        return userRepositoryPort.countAll()
                .flatMap(userCount -> {
                    if (userCount > 0) {
                        return Mono.error(new ConflictException("El sistema ya fue inicializado"));
                    }
                    return companyRepositoryPort.save(company)
                            .flatMap(savedCompany -> {
                                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
                                admin.setIsActive(true);
                                admin.setRole(Role.SUPER_ADMIN);
                                admin.setCompanyId(savedCompany.getId());
                                // Stock operations need somewhere to put stock, so every company starts with one warehouse.
                                return warehouseRepositoryPort.save(Warehouse.builder()
                                                .companyId(savedCompany.getId())
                                                .name("Almacén principal")
                                                .isActive(true)
                                                .build())
                                        .then(Mono.defer(() -> userRepositoryPort.save(admin)))
                                        .flatMap(this::issueAuthResponse);
                            });
                });
    }

    @Override
    public Mono<AuthResponse> refresh(String refreshToken) {
        return findValidRefreshToken(refreshToken)
                .flatMap(stored -> userRepositoryPort.findById(stored.getUserId())
                        .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                        .switchIfEmpty(Mono.error(invalidRefreshToken()))
                        .flatMap(user -> refreshTokenRepositoryPort.revokeById(stored.getId())
                                .then(issueAuthResponse(user))));
    }

    @Override
    public Mono<Void> logout(String refreshToken) {
        return findValidRefreshToken(refreshToken)
                .flatMap(stored -> refreshTokenRepositoryPort.revokeById(stored.getId()))
                .then();
    }

    @Override
    public Mono<User> me() {
        return SecurityHelper.currentUser()
                .switchIfEmpty(Mono.error(new UnauthorizedException("Debes iniciar sesión")))
                .flatMap(authUser -> userRepositoryPort.findById(authUser.getUserId()))
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .switchIfEmpty(Mono.error(new UnauthorizedException("Debes iniciar sesión")))
                .flatMap(this::withWarehouses)
                .flatMap(this::withCompany);
    }

    /** The warehouses the user is limited to (empty = all), so the UI can adapt. Admins are never limited. */
    private Mono<User> withWarehouses(User user) {
        if (user.getRole() == null || user.getRole().isAdmin()) {
            user.setWarehouseIds(List.of());
            return Mono.just(user);
        }
        return userRepositoryPort.findWarehouseIds(List.of(user.getId()))
                .map(byUser -> {
                    user.setWarehouseIds(byUser.getOrDefault(user.getId(), List.of()));
                    return user;
                });
    }

    private Mono<User> withCompany(User user) {
        if (user.getCompanyId() == null) {
            return Mono.just(user);
        }
        return companyRepositoryPort.findById(user.getCompanyId())
                .map(company -> {
                    user.setCompany(company);
                    return user;
                })
                .defaultIfEmpty(user);
    }

    private Mono<RefreshToken> findValidRefreshToken(String refreshToken) {
        return refreshTokenRepositoryPort.findValidByTokenHash(RefreshTokenHasher.hash(refreshToken))
                .switchIfEmpty(Mono.error(invalidRefreshToken()));
    }

    private Mono<AuthResponse> issueAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getId(), user.getCompanyId(), user.getRole());
        String rawRefreshToken = RefreshTokenGenerator.generate();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(RefreshTokenHasher.hash(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.refreshExpirationMs())))
                .build();

        return refreshTokenRepositoryPort.save(refreshToken)
                .map(saved -> new AuthResponse(
                        accessToken,
                        rawRefreshToken,
                        user.getId(),
                        user.getEmail(),
                        user.getCompanyId(),
                        user.getRole()
                ));
    }

    private UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("La sesión expiró; vuelve a iniciar sesión");
    }
}
