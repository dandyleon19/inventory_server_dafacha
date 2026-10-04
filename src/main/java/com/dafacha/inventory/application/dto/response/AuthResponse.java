package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.Role;

public record AuthResponse(
        String token,
        String refreshToken,
        Long userId,
        String email,
        Long companyId,
        Role role
) {
}
