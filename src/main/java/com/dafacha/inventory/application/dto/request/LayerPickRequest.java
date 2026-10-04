package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** How much to take from one purchase (cost layer). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LayerPickRequest {

    @NotNull(message = "La compra es obligatoria")
    @Positive(message = "La compra no es válida")
    private Long layerId;

    @NotNull(message = "La cantidad de la compra es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad de cada compra debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;
}
