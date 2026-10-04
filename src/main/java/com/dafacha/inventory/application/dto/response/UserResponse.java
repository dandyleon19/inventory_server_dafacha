package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        Boolean isActive,
        Role role,
        Long companyId,
        String companyName,
        /** The warehouses the user is limited to; empty means all of them. */
        List<Long> warehouseIds,
        LocalDateTime createdAt
) {
}
