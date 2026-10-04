package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.AuthResponse;
import com.dafacha.inventory.domain.model.Company;
import com.dafacha.inventory.domain.model.User;
import reactor.core.publisher.Mono;

public interface AuthUseCase {
    Mono<AuthResponse> login(String email, String password);
    Mono<AuthResponse> bootstrap(User admin, Company company);
    Mono<AuthResponse> refresh(String refreshToken);
    Mono<Void> logout(String refreshToken);
    Mono<User> me();
}
