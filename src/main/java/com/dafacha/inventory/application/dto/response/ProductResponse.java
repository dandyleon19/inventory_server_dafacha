package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.UnitOfMeasure;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        String sku,
        String barcode,
        UnitOfMeasure unit,
        BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        BigDecimal minStock,
        Boolean tracksExpiry,
        Boolean isActive
) {
}
