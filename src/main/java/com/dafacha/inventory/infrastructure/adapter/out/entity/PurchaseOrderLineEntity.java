package com.dafacha.inventory.infrastructure.adapter.out.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_order_lines")
public class PurchaseOrderLineEntity {

    @Id
    private Long id;

    @Column("purchase_order_id")
    private Long purchaseOrderId;
    @Column("product_id")
    private Long productId;

    private BigDecimal quantity;

    @Column("unit_cost")
    private BigDecimal unitCost;
    @Column("received_quantity")
    private BigDecimal receivedQuantity;
    private String packaging;
    @Column("entry_input")
    private String entryInput;
}
