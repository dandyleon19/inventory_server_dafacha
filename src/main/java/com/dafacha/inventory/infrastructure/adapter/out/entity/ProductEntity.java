package com.dafacha.inventory.infrastructure.adapter.out.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "products")
public class ProductEntity {

    @Id
    private Long id;

    @Column("company_id")
    private Long companyId;
    @Column("category_id")
    private Long categoryId;

    private String name;
    private String description;
    private String sku;
    private String barcode;
    private String unit;

    @Column("cost_price")
    private BigDecimal costPrice;
    @Column("sale_price")
    private BigDecimal salePrice;
    @Column("min_stock")
    private BigDecimal minStock;
    @Column("tracks_expiry")
    private Boolean tracksExpiry;
    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
