package com.iglesia.seguridad.usuario.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.rol.model.RolBase;

@Schema(description = "Perfil con el que puede navegar el usuario: uno por cada rol activo, con sus permisos.")
public record PerfilResponse(
		@Schema(description = "Identificador del rol.", example = "7") Long id,
		@Schema(description = "Nombre del rol.", example = "Secretaría") String nombre,
		@Schema(description = "Rol base: define la página de inicio y el ámbito del perfil.",
				example = "ADMIN_IGLESIA") RolBase rolBase,
		@Schema(description = "Permisos del rol, ordenados.",
				example = "[\"miembros.crear\", \"miembros.ver\"]") List<String> permisos) {
}
