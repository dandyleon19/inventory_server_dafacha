package com.dafacha.inventory.application.dto.request;

import com.dafacha.inventory.domain.model.UnitOfMeasure;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @Positive(message = "La categoría no es válida")
    private Long categoryId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre admite como máximo 150 caracteres")
    private String name;

    @Size(max = 2000, message = "La descripción admite como máximo 2000 caracteres")
    private String description;

    // Whether SKU / category / barcode are mandatory is a company setting, enforced in ProductServiceImpl.
    @Size(max = 50, message = "El SKU admite como máximo 50 caracteres")
    private String sku;

    @Size(max = 50, message = "El código de barras admite como máximo 50 caracteres")
    private String barcode;

    @NotNull(message = "La unidad de medida es obligatoria")
    private UnitOfMeasure unit;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
    @Digits(integer = 8, fraction = 4, message = "El costo admite como máximo 8 enteros y 4 decimales")
    private BigDecimal costPrice;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio de venta admite como máximo 10 enteros y 2 decimales")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "El stock mínimo no puede ser negativo")
    @Digits(integer = 9, fraction = 3, message = "El stock mínimo admite como máximo 9 enteros y 3 decimales")
    private BigDecimal minStock;

    /** Keep this product's stock in lots with an expiry date. Defaults to false. */
    private Boolean tracksExpiry;

    private Boolean isActive;
}
