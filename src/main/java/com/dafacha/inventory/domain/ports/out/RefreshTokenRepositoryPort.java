package com.dafacha.inventory.domain.ports.out;

import com.dafacha.inventory.domain.model.RefreshToken;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepositoryPort {

    Mono<RefreshToken> save(RefreshToken refreshToken);

    Mono<RefreshToken> findValidByTokenHash(String tokenHash);

    Mono<Void> revokeById(Long id);

    /** Ends every session of the user (used when the user is deactivated or the password is reset). */
    Mono<Void> revokeAllByUserId(Long userId);
}
