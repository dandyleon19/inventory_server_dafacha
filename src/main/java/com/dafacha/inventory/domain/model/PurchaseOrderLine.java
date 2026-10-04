package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderLine {
    private Long id;
    private Long purchaseOrderId;
    private Long productId;
    private BigDecimal quantity;
    /** Cost per unit of the product's own unit, as agreed with the supplier. */
    private BigDecimal unitCost;
    private BigDecimal receivedQuantity;
    /** How the line was bought, in the user's words ("1 paquete de 12 + 6 sueltas"). Informative only. */
    private String packaging;
    /** What the user typed to describe the line, as the form wrote it (opaque to the server). */
    private String entryInput;

    // Display fields filled by the detail query (join with products).
    private String productName;
    private String productSku;
    private UnitOfMeasure unit;
    private Boolean tracksExpiry;

    /** What is still to arrive. */
    public BigDecimal getPendingQuantity() {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        return quantity.subtract(receivedQuantity == null ? BigDecimal.ZERO : receivedQuantity);
    }

    public BigDecimal getLineTotal() {
        if (quantity == null || unitCost == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
    }
}
