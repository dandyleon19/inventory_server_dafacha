package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** What was counted in one lot. A missing {@code lotId} means the units without an expiry date. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LotCountRequest {

    private Long lotId;

    @NotNull(message = "La cantidad contada del lote es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad contada no puede ser negativa")
    @Digits(integer = 11, fraction = 3, message = "La cantidad contada admite como máximo 11 enteros y 3 decimales")
    private BigDecimal countedQuantity;
}
