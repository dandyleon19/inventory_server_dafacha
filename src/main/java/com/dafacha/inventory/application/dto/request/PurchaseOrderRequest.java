package com.dafacha.inventory.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "El proveedor es obligatorio")
    @Positive(message = "El proveedor no es válido")
    private Long supplierId;

    @NotNull(message = "El almacén es obligatorio")
    @Positive(message = "El almacén no es válido")
    private Long warehouseId;

    private LocalDate expectedDate;

    @Size(max = 1000, message = "Las notas admiten como máximo 1000 caracteres")
    private String notes;

    @NotEmpty(message = "Se necesita al menos una línea")
    @Valid
    private List<PurchaseOrderLineRequest> lines;
}
