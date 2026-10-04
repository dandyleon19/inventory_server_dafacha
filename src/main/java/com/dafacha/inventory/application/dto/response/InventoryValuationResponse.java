package com.dafacha.inventory.application.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record InventoryValuationResponse(
        BigDecimal totalValue,
        List<WarehouseValuationResponse> warehouses
) {
}
