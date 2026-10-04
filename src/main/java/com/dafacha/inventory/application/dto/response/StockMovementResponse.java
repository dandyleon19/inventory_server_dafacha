package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.MovementType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Names are filled in the history list; the movements returned right after an operation carry ids only. */
@Builder
public record StockMovementResponse(
        Long id,
        Long warehouseId,
        String warehouseName,
        Long productId,
        String productName,
        String productSku,
        MovementType type,
        BigDecimal quantity,
        BigDecimal balanceAfter,
        BigDecimal unitCost,
        String notes,
        String transferRef,
        Long purchaseOrderId,
        String purchaseOrderNumber,
        String supplierName,
        String lotNumber,
        LocalDate expiryDate,
        Long userId,
        String userName,
        LocalDateTime createdAt
) {
}
