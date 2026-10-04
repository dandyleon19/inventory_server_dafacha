package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One purchase / entry of a product into a warehouse, with the unit cost it really had and how much of it is
 * left. The value of the inventory is the sum of quantity x unitCost over the layers.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CostLayer {
    private Long id;
    private Long companyId;
    private Long warehouseId;
    private Long productId;
    private BigDecimal unitCost;
    /** What is left of the layer. */
    private BigDecimal quantity;
    /** The lot it belongs to (products that control expiry); null for units without a lot. */
    private LocalDate expiryDate;
    /** Empty when the lot has no number. */
    private String lotNumber;
    private LocalDateTime receivedAt;
}
