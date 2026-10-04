package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.UnitOfMeasure;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PurchaseOrderLineResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        UnitOfMeasure unit,
        Boolean tracksExpiry,
        BigDecimal quantity,
        BigDecimal unitCost,
        String packaging,
        String entryInput,
        BigDecimal receivedQuantity,
        BigDecimal pendingQuantity,
        BigDecimal lineTotal
) {
}
