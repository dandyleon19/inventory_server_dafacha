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
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "El almacén es obligatorio")
    @Positive(message = "El almacén no es válido")
    private Long warehouseId;

    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto no es válido")
    private Long productId;

    @NotNull(message = "La cantidad contada es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad contada no puede ser negativa")
    @Digits(integer = 11, fraction = 3, message = "La cantidad contada admite como máximo 11 enteros y 3 decimales")
    private BigDecimal countedQuantity;

    /** Needed when the count is higher than the stock and the product tracks expiry: the lot being added. */
    private LocalDate expiryDate;

    @Size(max = 50, message = "El número de lote admite como máximo 50 caracteres")
    private String lotNumber;

    @Size(max = 500, message = "Las notas admiten como máximo 500 caracteres")
    private String notes;

    /** Expiry-tracked product counted lot by lot. {@code countedQuantity} must equal their total plus {@code newLots}. */
    @Valid
    private List<LotCountRequest> lotCounts;

    /** Lots found in the count that the system did not have. */
    @Valid
    private List<NewLotRequest> newLots;
}
