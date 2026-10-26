package com.iglesia.seguridad.usuario.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario autenticado con sus perfiles. Nunca incluye la contraseña ni su hash.")
public record MiUsuarioResponse(
		@Schema(description = "Identificador del usuario (claim `sub` del JWT).", example = "12") Long id,
		@Schema(description = "Correo con el que inicia sesión.", example = "secretaria@iglesia.org") String correo,
		@Schema(description = "Iglesia del usuario; nulo para `ADMIN_SISTEMA`.", example = "1",
				nullable = true) Long iglesiaId,
		@Schema(description = "Miembro de iglesia-api vinculado, si tiene.", example = "25",
				nullable = true) Long miembroId,
		@Schema(description = "Un perfil por cada rol activo, ordenados por la jerarquía de su rol base y luego "
				+ "por nombre. Los roles desactivados no aparecen.",
				example = "[{\"id\": 7, \"nombre\": \"Secretaría\", \"rolBase\": \"ADMIN_IGLESIA\", "
						+ "\"permisos\": [\"miembros.crear\", \"miembros.ver\"]}]") List<PerfilResponse> perfiles) {
}
