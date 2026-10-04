package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMovement {
    private Long id;
    private Long companyId;
    private Long warehouseId;
    private Long productId;
    private MovementType type;
    /** Signed: positive = stock in, negative = stock out. */
    private BigDecimal quantity;
    /** Stock of this product in this warehouse right after the movement. */
    private BigDecimal balanceAfter;
    /** Snapshot of the product's cost when the movement happened. */
    private BigDecimal unitCost;
    private String notes;
    /** Shared by the two legs of a transfer. */
    private String transferRef;
    /** Set when the movement is goods received against a purchase order. */
    private Long purchaseOrderId;
    /** Set when the movement touched a lot of an expiry-tracked product. */
    private String lotNumber;
    private LocalDate expiryDate;
    private Long userId;
    private LocalDateTime createdAt;

    // Display fields filled by the list query (joins); not stored on the movement row.
    private String warehouseName;
    private String productName;
    private String productSku;
    private String userName;
    private String purchaseOrderNumber;
    /** Supplier of the purchase order this movement belongs to (receipts only). */
    private String supplierName;
}
