package com.dafacha.inventory.application.dto.request;

import com.dafacha.inventory.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Body for creating (POST) and editing (PUT) a user; the password is only used, and required, on create. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveUserRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 255, message = "Los nombres admiten como máximo 255 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 255, message = "Los apellidos admiten como máximo 255 caracteres")
    private String lastName;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 255, message = "El correo admite como máximo 255 caracteres")
    private String email;

    @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    private Boolean isActive;

    /** The warehouses the user is limited to. Empty or missing = all warehouses (ignored for admins). */
    private List<Long> warehouseIds;
}
