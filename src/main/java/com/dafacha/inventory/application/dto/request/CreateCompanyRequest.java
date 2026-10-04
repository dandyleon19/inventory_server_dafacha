package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite como máximo 100 caracteres")
    private String name;

    @Size(max = 100, message = "La razón social admite como máximo 100 caracteres")
    private String socialReason;

    @Size(max = 100, message = "La dirección fiscal admite como máximo 100 caracteres")
    private String fiscalAddress;

    @Size(max = 20, message = "El RUC admite como máximo 20 caracteres")
    private String rucNumber;

    @Size(max = 20, message = "El teléfono admite como máximo 20 caracteres")
    private String phone;
}
