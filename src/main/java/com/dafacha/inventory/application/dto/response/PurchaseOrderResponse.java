package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.PurchaseOrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** `lines` is filled in the detail; the list returns `lineCount` instead. */
@Builder
public record PurchaseOrderResponse(
        Long id,
        String number,
        PurchaseOrderStatus status,
        Long supplierId,
        String supplierName,
        Long warehouseId,
        String warehouseName,
        LocalDate expectedDate,
        String notes,
        BigDecimal total,
        Integer lineCount,
        String createdByName,
        LocalDateTime orderedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        List<PurchaseOrderLineResponse> lines
) {
}
