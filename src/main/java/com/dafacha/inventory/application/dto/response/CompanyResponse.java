package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

@Builder
public record CompanyResponse(
        Long id,
        String name,
        String socialReason,
        String fiscalAddress,
        String rucNumber,
        String phone
) {
}
