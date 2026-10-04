package com.dafacha.inventory.application.dto.request;

import jakarta.validation.Valid;
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
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferRequest {

    @NotNull(message = "El almacén de origen es obligatorio")
    @Positive(message = "El almacén de origen no es válido")
    private Long fromWarehouseId;

    @NotNull(message = "El almacén de destino es obligatorio")
    @Positive(message = "El almacén de destino no es válido")
    private Long toWarehouseId;

    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto no es válido")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;

    @Size(max = 500, message = "Las notas admiten como máximo 500 caracteres")
    private String notes;

    /** Expiry-tracked product: the lots to move. Missing = the ones that expire first. */
    @Valid
    private List<LotPickRequest> lots;

    /** Product without expiry control: which purchases it comes from (cost method MANUAL). */
    @Valid
    private List<LayerPickRequest> layers;
}
