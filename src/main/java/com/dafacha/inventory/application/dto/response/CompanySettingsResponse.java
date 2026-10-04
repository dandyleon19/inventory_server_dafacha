package com.dafacha.inventory.application.dto.response;

import com.dafacha.inventory.domain.model.CostMethod;
import lombok.Builder;

@Builder
public record CompanySettingsResponse(
        Boolean requireProductSku,
        Boolean requireProductCategory,
        Boolean requireProductBarcode,
        Boolean allowNegativeStock,
        Integer expiryAlertDays,
        Integer purchaseArrivalAlertDays,
        CostMethod costMethod
) {
}
