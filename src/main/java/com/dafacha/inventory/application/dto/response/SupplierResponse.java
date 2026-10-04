package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

@Builder
public record SupplierResponse(
        Long id,
        String name,
        String rucNumber,
        String contactName,
        String phone,
        String email,
        String address,
        String notes,
        Boolean isActive
) {
}
