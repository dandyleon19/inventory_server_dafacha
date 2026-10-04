package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrder {
    private Long id;
    private Long companyId;
    private Long supplierId;
    /** Warehouse that receives the goods. */
    private Long warehouseId;
    /** Human-readable, per company: OC-000001. */
    private String number;
    private PurchaseOrderStatus status;
    private LocalDate expectedDate;
    private String notes;
    private BigDecimal total;
    private Long createdBy;
    private LocalDateTime orderedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<PurchaseOrderLine> lines;

    // Display fields filled by the read queries (joins); not stored on the order row.
    private String supplierName;
    private String warehouseName;
    private String createdByName;
    /** Only filled by the list query (the detail has the lines themselves). */
    private Integer lineCount;

    /** Sum of the lines (each rounded to cents), kept on the order so lists don't have to add lines up. */
    public static BigDecimal totalOf(List<PurchaseOrderLine> lines) {
        return lines.stream()
                .map(PurchaseOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
