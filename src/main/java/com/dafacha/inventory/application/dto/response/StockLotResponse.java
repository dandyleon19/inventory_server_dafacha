package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.UnitOfMeasure;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record StockLotResponse(
        Long id,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productName,
        String productSku,
        UnitOfMeasure unit,
        String lotNumber,
        LocalDate expiryDate,
        BigDecimal quantity,
        /** Negative when already expired. */
        Integer daysToExpiry
) {
}
