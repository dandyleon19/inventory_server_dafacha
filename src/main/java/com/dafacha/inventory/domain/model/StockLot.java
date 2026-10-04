package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** What is left of one batch (same product, warehouse, expiry date and lot number). */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockLot {
    private Long id;
    private Long companyId;
    private Long warehouseId;
    private Long productId;
    /** Empty string when the lot has no number. */
    private String lotNumber;
    private LocalDate expiryDate;
    private BigDecimal quantity;

    // Display fields filled by the list query.
    private String warehouseName;
    private String productName;
    private String productSku;
    private UnitOfMeasure unit;
    /** Negative when already expired. Computed by the database so it uses its own "today". */
    private Integer daysToExpiry;
}
