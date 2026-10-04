package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineRequest {

    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto no es válido")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
    @Digits(integer = 8, fraction = 4, message = "El costo unitario admite como máximo 8 enteros y 4 decimales")
    private BigDecimal unitCost;

    /** How the line was bought ("1 paquete de 12 + 6 sueltas"); only informative. */
    @Size(max = 200, message = "La descripción de la compra admite como máximo 200 caracteres")
    private String packaging;

    /** The form's own description of how the line was entered; stored as is so editing restores it. */
    @Size(max = 1000, message = "El detalle de la línea admite como máximo 1000 caracteres")
    private String entryInput;
}
