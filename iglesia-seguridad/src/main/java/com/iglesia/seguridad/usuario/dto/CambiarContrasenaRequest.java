package com.iglesia.seguridad.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;

@Schema(description = "Cambio de la contraseña propia.")
public record CambiarContrasenaRequest(
		@Schema(description = "Contraseña actual, para confirmar la identidad.", example = "ClaveSegura123",
				format = "password") @NotBlank String contrasenaActual,
		@Schema(description = "Nueva contraseña.", example = "OtraClaveSegura456", format = "password") @NotBlank @Size(
				min = LimitesUsuario.CONTRASENA_MIN, max = LimitesUsuario.CONTRASENA_MAX) String contrasenaNueva) {
}
