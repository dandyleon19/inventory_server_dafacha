package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.CostLayer;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One purchase of a product in a warehouse, with what is left of it. */
@Builder
public record CostLayerResponse(
        Long id,
        BigDecimal unitCost,
        BigDecimal quantity,
        LocalDate expiryDate,
        String lotNumber,
        LocalDateTime receivedAt
) {

    public static CostLayerResponse from(CostLayer layer) {
        return CostLayerResponse.builder()
                .id(layer.getId())
                .unitCost(layer.getUnitCost())
                .quantity(layer.getQuantity())
                .expiryDate(layer.getExpiryDate())
                .lotNumber(layer.getLotNumber())
                .receivedAt(layer.getReceivedAt())
                .build();
    }
}
