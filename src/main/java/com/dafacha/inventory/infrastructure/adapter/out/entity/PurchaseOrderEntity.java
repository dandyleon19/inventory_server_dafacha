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
@Table(name = "purchase_orders")
public class PurchaseOrderEntity {

    @Id
    private Long id;

    @Column("company_id")
    private Long companyId;
    @Column("supplier_id")
    private Long supplierId;
    @Column("warehouse_id")
    private Long warehouseId;

    private String number;
    private String status;

    @Column("expected_date")
    private LocalDate expectedDate;

    private String notes;
    private BigDecimal total;

    @Column("created_by")
    private Long createdBy;
    @Column("ordered_at")
    private LocalDateTime orderedAt;
    @Column("cancelled_at")
    private LocalDateTime cancelledAt;

    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
