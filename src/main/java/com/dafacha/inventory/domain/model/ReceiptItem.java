package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of a goods receipt: how much of a purchase order line arrived. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptItem {
    private Long lineId;
    private BigDecimal quantity;
    /** Required when the line's product tracks expiry: the lot that arrived. */
    private LocalDate expiryDate;
    private String lotNumber;
}
