package com.dafacha.inventory.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapRequest {

    @NotNull(message = "Faltan los datos de la empresa")
    @Valid
    private CreateCompanyRequest company;

    @NotNull(message = "Faltan los datos del usuario administrador")
    @Valid
    private CreateUserRequest admin;
}
