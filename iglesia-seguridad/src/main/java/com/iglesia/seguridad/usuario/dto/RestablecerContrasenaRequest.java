package com.iglesia.seguridad.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;

@Schema(description = "Contraseña temporal que un administrador fija a otro usuario. El usuario debe cambiarla "
		+ "en su próximo inicio de sesión.")
public record RestablecerContrasenaRequest(
		@Schema(description = "Contraseña temporal que el administrador comunica al usuario.", example = "Temporal123",
				format = "password") @NotBlank @Size(min = LimitesUsuario.CONTRASENA_MIN,
						max = LimitesUsuario.CONTRASENA_MAX) String contrasenaTemporal) {
}
