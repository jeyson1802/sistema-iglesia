package com.iglesia.seguridad.usuario.dto;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.usuario.model.Rol;

@Schema(description = "Usuario del sistema. Nunca incluye la contraseña ni su hash.")
public record UsuarioResponse(
		@Schema(description = "Identificador del usuario (claim `sub` del JWT).", example = "12") Long id,
		@Schema(description = "Correo con el que inicia sesión.", example = "lider@iglesia.org") String correo,
		@Schema(description = "Iglesia del usuario; nulo para `ADMIN_SISTEMA`.", example = "1",
				nullable = true) Long iglesiaId,
		@Schema(description = "Miembro de iglesia-api vinculado, si tiene.", example = "25",
				nullable = true) Long miembroId,
		@Schema(description = "Si puede iniciar sesión.", example = "true") boolean activo,
		@Schema(description = "Roles de acceso del usuario.", example = "[\"LIDER\"]") Set<Rol> roles,
		@Schema(description = "Fecha y hora de creación (UTC).", example = "2026-10-26T14:30:00Z") Instant creadoEn) {
}
