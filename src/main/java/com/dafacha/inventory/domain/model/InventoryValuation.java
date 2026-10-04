package com.dafacha.inventory.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryValuation {
    private BigDecimal totalValue;
    private List<WarehouseValuation> warehouses;
}
