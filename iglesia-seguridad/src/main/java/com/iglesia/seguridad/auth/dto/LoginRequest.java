package com.iglesia.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciales para iniciar sesión.")
public record LoginRequest(
		@Schema(description = "Correo del usuario; no distingue mayúsculas.", example = "admin@iglesia.org",
				format = "email") @NotBlank @Size(max = 254) String correo,
		@Schema(description = "Contraseña del usuario.", example = "ClaveSegura123",
				format = "password") @NotBlank @Size(max = 72) String contrasena) {

	/** Evita que la contraseña aparezca en logs o trazas por el toString del record. */
	@Override
	public String toString() {
		return "LoginRequest[correo=***, contrasena=***]";
	}

}
