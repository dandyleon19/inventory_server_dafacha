package com.dafacha.inventory.infrastructure.adapter.out.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "company_settings")
public class CompanySettingsEntity {

    @Id
    @Column("company_id")
    private Long companyId;

    @Column("require_product_sku")
    private Boolean requireProductSku;
    @Column("require_product_category")
    private Boolean requireProductCategory;
    @Column("require_product_barcode")
    private Boolean requireProductBarcode;
    @Column("allow_negative_stock")
    private Boolean allowNegativeStock;
    @Column("expiry_alert_days")
    private Integer expiryAlertDays;
    @Column("purchase_arrival_alert_days")
    private Integer purchaseArrivalAlertDays;
    @Column("cost_method")
    private String costMethod;

    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
