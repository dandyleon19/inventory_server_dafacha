package com.dafacha.inventory.infrastructure.adapter.out.repository;

import com.dafacha.inventory.infrastructure.adapter.out.entity.CompanySettingsEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface CompanySettingsRepository extends R2dbcRepository<CompanySettingsEntity, Long> {

    /**
     * Explicit upsert: the primary key is the company id (always non-null), so Spring Data's save()
     * would treat the entity as existing and issue an UPDATE that matches no row on the first save.
     */
    @Modifying
    @Query("""
            INSERT INTO company_settings (company_id, require_product_sku, require_product_category,
                                          require_product_barcode, allow_negative_stock,
                                          expiry_alert_days, purchase_arrival_alert_days, cost_method)
            VALUES (:companyId, :requireSku, :requireCategory, :requireBarcode, :allowNegativeStock,
                    :expiryAlertDays, :purchaseArrivalAlertDays, :costMethod)
            ON CONFLICT (company_id) DO UPDATE SET
                require_product_sku = :requireSku,
                require_product_category = :requireCategory,
                require_product_barcode = :requireBarcode,
                allow_negative_stock = :allowNegativeStock,
                expiry_alert_days = :expiryAlertDays,
                purchase_arrival_alert_days = :purchaseArrivalAlertDays,
                cost_method = :costMethod,
                updated_at = CURRENT_TIMESTAMP
            """)
    Mono<Integer> upsert(Long companyId, boolean requireSku, boolean requireCategory, boolean requireBarcode,
                         boolean allowNegativeStock, int expiryAlertDays, int purchaseArrivalAlertDays,
                         String costMethod);
}
