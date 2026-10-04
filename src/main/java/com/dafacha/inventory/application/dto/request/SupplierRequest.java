package com.dafacha.inventory.application.dto.request;

import jakarta.validation.constraints.Email;
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
public class SupplierRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre admite como máximo 150 caracteres")
    private String name;

    @Size(max = 20, message = "El RUC admite como máximo 20 caracteres")
    private String rucNumber;

    @Size(max = 100, message = "El contacto admite como máximo 100 caracteres")
    private String contactName;

    @Size(max = 20, message = "El teléfono admite como máximo 20 caracteres")
    private String phone;

    @Email(message = "El correo no es válido")
    @Size(max = 100, message = "El correo admite como máximo 100 caracteres")
    private String email;

    @Size(max = 200, message = "La dirección admite como máximo 200 caracteres")
    private String address;

    @Size(max = 1000, message = "Las notas admiten como máximo 1000 caracteres")
    private String notes;

    private Boolean isActive;
}
