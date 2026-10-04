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
import java.time.LocalDate;

/** Body for stock entries and exits. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequest {

    @NotNull(message = "El almacén es obligatorio")
    @Positive(message = "El almacén no es válido")
    private Long warehouseId;

    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto no es válido")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite como máximo 11 enteros y 3 decimales")
    private BigDecimal quantity;

    /** Required for entries of a product that tracks expiry; ignored for exits. */
    private LocalDate expiryDate;

    @Size(max = 50, message = "El número de lote admite como máximo 50 caracteres")
    private String lotNumber;

    @Size(max = 500, message = "Las notas admiten como máximo 500 caracteres")
    private String notes;

    /** Exits of an expiry-tracked product: the lots to take from. Missing = the ones that expire first. */
    @Valid
    private List<LotPickRequest> lots;

    /** Entries: what each unit cost. Missing = the product's current cost. */
    @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
    @Digits(integer = 8, fraction = 4, message = "El costo unitario admite como máximo 8 enteros y 4 decimales")
    private BigDecimal unitCost;

    /** Exits of a product without expiry control: which purchases it comes from (cost method MANUAL). */
    @Valid
    private List<LayerPickRequest> layers;
}
