package com.iglesia.seguridad.usuario.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Usuario autenticado con sus perfiles. Nunca incluye la contraseña ni su hash.")
public record MiUsuarioResponse(
		@Schema(description = "Identificador del usuario (claim `sub` del JWT).", example = "12") Long id,
		@Schema(description = "Correo con el que inicia sesión.", example = "secretaria@iglesia.org") String correo,
		@Schema(description = "Iglesia en la que opera: la del usuario o, para `ADMIN_SISTEMA`, la iglesia a la "
				+ "que entró con `POST /api/auth/iglesia-actuando`. Nulo para `ADMIN_SISTEMA` en la vista global.",
				example = "1", nullable = true) Long iglesiaId,
		@Schema(description = "Miembro de iglesia-api vinculado, si tiene.", example = "25",
				nullable = true) Long miembroId,
		@Schema(description = "`true` si su contraseña es temporal: mientras no la cambie con "
				+ "`PUT /api/usuarios/me/contrasena`, el resto de endpoints responde 403.", example = "false")
		boolean debeCambiarContrasena,
		@Schema(description = "`true` si es `ADMIN_SISTEMA` operando dentro de una iglesia; su perfil lleva entonces "
				+ "también los permisos de un administrador de iglesia.", example = "false") boolean actuandoEnIglesia,
		@Schema(description = "Un perfil por cada rol activo, ordenados por la jerarquía de su rol base y luego "
				+ "por nombre. Los roles desactivados no aparecen.",
				example = "[{\"id\": 7, \"nombre\": \"Secretaría\", \"rolBase\": \"ADMIN_IGLESIA\", "
						+ "\"permisos\": [\"miembros.crear\", \"miembros.ver\"]}]") List<PerfilResponse> perfiles) {
}
