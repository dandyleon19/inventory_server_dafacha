package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long id;
    private Long companyId;
    private Long categoryId;
    /** Not persisted on the product row; filled from the category when reading. */
    private String categoryName;
    private String name;
    private String description;
    private String sku;
    private String barcode;
    private UnitOfMeasure unit;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private BigDecimal minStock;
    /** When true, stock of this product is kept in lots with an expiry date and consumed first-expired-first-out. */
    private Boolean tracksExpiry;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Sale price minus cost price, or null when either is missing. */
    public BigDecimal getMarginAmount() {
        if (costPrice == null || salePrice == null) {
            return null;
        }
        return salePrice.subtract(costPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /** Margin over the sale price, as a percentage; null when there is no sale price to divide by. */
    public BigDecimal getMarginPercent() {
        if (costPrice == null || salePrice == null || salePrice.signum() == 0) {
            return null;
        }
        return salePrice.subtract(costPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(salePrice, 2, RoundingMode.HALF_UP);
    }
}
