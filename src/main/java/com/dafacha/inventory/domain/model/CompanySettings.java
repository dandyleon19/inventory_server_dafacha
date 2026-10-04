package com.dafacha.inventory.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanySettings {
    private Long companyId;
    private Boolean requireProductSku;
    private Boolean requireProductCategory;
    private Boolean requireProductBarcode;
    /** When false (default), an exit or transfer larger than the available stock is rejected. */
    private Boolean allowNegativeStock;
    /** Lots expiring within this many days are "expiring soon". */
    private Integer expiryAlertDays;
    /** Open purchase orders expected within this many days are "arriving soon". */
    private Integer purchaseArrivalAlertDays;
    /** How the cost of what leaves is decided: the oldest purchase first (FIFO), or the user picks (MANUAL). */
    private CostMethod costMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** What a company gets until it saves its own settings. Keep in sync with the V3 migration. */
    public static CompanySettings defaults(Long companyId) {
        return CompanySettings.builder()
                .companyId(companyId)
                .requireProductSku(false)
                .requireProductCategory(false)
                .requireProductBarcode(false)
                .allowNegativeStock(false)
                .expiryAlertDays(30)
                .purchaseArrivalAlertDays(7)
                .costMethod(CostMethod.FIFO)
                .build();
    }
}
