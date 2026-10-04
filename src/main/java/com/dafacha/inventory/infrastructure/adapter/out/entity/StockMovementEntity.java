package com.dafacha.inventory.infrastructure.adapter.out.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "stock_movements")
public class StockMovementEntity {

    @Id
    private Long id;

    @Column("company_id")
    private Long companyId;
    @Column("warehouse_id")
    private Long warehouseId;
    @Column("product_id")
    private Long productId;

    private String type;
    private BigDecimal quantity;

    @Column("balance_after")
    private BigDecimal balanceAfter;
    @Column("unit_cost")
    private BigDecimal unitCost;

    private String notes;

    @Column("transfer_ref")
    private String transferRef;
    @Column("purchase_order_id")
    private Long purchaseOrderId;
    @Column("lot_number")
    private String lotNumber;
    @Column("expiry_date")
    private LocalDate expiryDate;
    @Column("user_id")
    private Long userId;

    @Column("created_at")
    private LocalDateTime createdAt;
}
