package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.UnitOfMeasure;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record StockSummaryResponse(
        Long productId,
        String sku,
        String productName,
        UnitOfMeasure unit,
        BigDecimal minStock,
        BigDecimal costPrice,
        Boolean tracksExpiry,
        Boolean isActive,
        BigDecimal quantity,
        BigDecimal value,
        Boolean lowStock
) {
}
