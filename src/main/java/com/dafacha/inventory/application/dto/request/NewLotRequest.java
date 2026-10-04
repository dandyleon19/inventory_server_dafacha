package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A lot found in a physical count that the system did not have. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewLotRequest {

    @NotNull(message = "La fecha de vencimiento del lote es obligatoria")
    private LocalDate expiryDate;

    @Size(max = 50, message = "El número de lote admite como máximo 50 caracteres")
    private String lotNumber;

    @NotNull(message = "La cantidad del lote es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad de cada lote debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;
}
