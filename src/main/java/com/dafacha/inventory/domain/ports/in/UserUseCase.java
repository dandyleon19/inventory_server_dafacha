package com.dafacha.inventory.domain.ports.in;

import com.dafacha.inventory.application.dto.response.PageResponse;
import com.dafacha.inventory.domain.model.Role;
import com.dafacha.inventory.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * User management for the caller's own company (admins only). Users are never deleted, since movements and the
 * audit trail point at them: they are deactivated instead.
 */
public interface UserUseCase {

    /** Uses firstName, lastName, email, password (required), role, warehouseIds. The user starts active. */
    Mono<User> create(User user);

    /** Uses firstName, lastName, email, role, isActive, warehouseIds. The password is untouched. */
    Mono<User> update(Long id, User changes);

    /** Sets a new password and ends all of the user's sessions. */
    Mono<Void> resetPassword(Long id, String newPassword);

    Mono<User> findById(Long id);

    Mono<PageResponse<User>> findAll(int page, int size, String search, Role role, Boolean isActive);
}
