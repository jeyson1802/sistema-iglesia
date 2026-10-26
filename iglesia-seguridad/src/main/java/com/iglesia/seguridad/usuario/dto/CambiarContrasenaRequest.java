package com.iglesia.seguridad.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;

public record CambiarContrasenaRequest(
		@NotBlank String contrasenaActual,
		@NotBlank @Size(min = LimitesUsuario.CONTRASENA_MIN, max = LimitesUsuario.CONTRASENA_MAX) String contrasenaNueva) {
}
