package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

@Builder
public record WarehouseResponse(
        Long id,
        String name,
        String address,
        Boolean isActive
) {
}
