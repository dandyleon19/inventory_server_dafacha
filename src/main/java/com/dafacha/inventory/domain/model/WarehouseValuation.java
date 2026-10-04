package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseValuation {
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal value;
    /** How many products currently have stock above zero in this warehouse. */
    private Long productCount;
}
