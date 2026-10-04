package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** How much to take from one lot. A missing {@code lotId} means the units without an expiry date. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LotPickRequest {

    private Long lotId;

    @NotNull(message = "La cantidad del lote es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad de cada lote debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;
}
