package com.iglesia.seguridad.usuario.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.iglesia.seguridad.usuario.Rol;

/** La iglesia de un usuario no cambia; el correo tampoco se edita por esta vía. */
public record ActualizarUsuarioRequest(
		@NotEmpty Set<@NotNull Rol> roles,
		@Positive Long miembroId,
		@NotNull Boolean activo) {
}
