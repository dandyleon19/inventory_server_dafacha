package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** One product's stock, summed over all warehouses or limited to one, with its valuation. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockSummary {
    private Long productId;
    private String sku;
    private String productName;
    private UnitOfMeasure unit;
    private BigDecimal minStock;
    private BigDecimal costPrice;
    private Boolean tracksExpiry;
    private Boolean isActive;
    private BigDecimal quantity;

    /** The real value of this stock: the sum over its cost layers (what each unit really cost). Null when unknown. */
    private BigDecimal realValue;

    /**
     * What the stock is worth. Normally the real value from the cost layers; without it (never the case once the
     * layers exist) it falls back to quantity x the catalog cost.
     */
    public BigDecimal getValue() {
        if (realValue != null) {
            return realValue.setScale(2, RoundingMode.HALF_UP);
        }
        if (quantity == null || costPrice == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return quantity.multiply(costPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /** Flagged only when a minimum is set (> 0) and the stock has fallen to it or below. */
    public Boolean getLowStock() {
        return minStock != null
                && quantity != null
                && minStock.signum() > 0
                && quantity.compareTo(minStock) <= 0;
    }
}
