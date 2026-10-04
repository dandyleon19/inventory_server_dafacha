package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record WarehouseValuationResponse(
        Long warehouseId,
        String warehouseName,
        BigDecimal value,
        Long productCount
) {
}
