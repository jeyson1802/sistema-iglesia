package com.iglesia.seguridad.usuario.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario del sistema. Nunca incluye la contraseña ni su hash.")
public record UsuarioResponse(
		@Schema(description = "Identificador del usuario (claim `sub` del JWT).", example = "12") Long id,
		@Schema(description = "Correo con el que inicia sesión.", example = "lider@iglesia.org") String correo,
		@Schema(description = "Iglesia del usuario; nulo para `ADMIN_SISTEMA`.", example = "1",
				nullable = true) Long iglesiaId,
		@Schema(description = "Miembro de iglesia-api vinculado, si tiene.", example = "25",
				nullable = true) Long miembroId,
		@Schema(description = "Si puede iniciar sesión.", example = "true") boolean activo,
		@Schema(description = "Roles de acceso del usuario (base y personalizados), ordenados por la jerarquía de "
				+ "su rol base y luego por nombre.",
				example = "[{\"id\": 7, \"nombre\": \"Secretaría\", \"rolBase\": \"ADMIN_IGLESIA\"}]")
		List<RolAsignadoResponse> roles,
		@Schema(description = "Fecha y hora de creación (UTC).", example = "2026-10-26T14:30:00Z") Instant creadoEn) {
}
