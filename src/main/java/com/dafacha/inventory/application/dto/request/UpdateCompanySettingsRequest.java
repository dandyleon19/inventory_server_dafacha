package com.dafacha.inventory.application.dto.request;

import com.dafacha.inventory.domain.model.CostMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanySettingsRequest {

    @NotNull(message = "Indica si el SKU es obligatorio")
    private Boolean requireProductSku;

    @NotNull(message = "Indica si la categoría es obligatoria")
    private Boolean requireProductCategory;

    @NotNull(message = "Indica si el código de barras es obligatorio")
    private Boolean requireProductBarcode;

    @NotNull(message = "Indica si se permite el stock negativo")
    private Boolean allowNegativeStock;

    @NotNull(message = "Los días de alerta de vencimiento son obligatorios")
    @Min(value = 1, message = "Los días de alerta de vencimiento deben ser al menos 1")
    @Max(value = 365, message = "Los días de alerta de vencimiento no pueden superar 365")
    private Integer expiryAlertDays;

    @NotNull(message = "Los días de alerta de llegada de compras son obligatorios")
    @Min(value = 1, message = "Los días de alerta de llegada de compras deben ser al menos 1")
    @Max(value = 90, message = "Los días de alerta de llegada de compras no pueden superar 90")
    private Integer purchaseArrivalAlertDays;

    @NotNull(message = "Indica el método de costo")
    private CostMethod costMethod;
}
