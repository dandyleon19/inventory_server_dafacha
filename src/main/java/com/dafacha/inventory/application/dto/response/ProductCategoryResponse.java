package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

@Builder
public record ProductCategoryResponse(
        Long id,
        String name,
        String description
) {
}
