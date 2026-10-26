package com.iglesia.seguridad.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.rol.model.RolBase;

@Schema(description = "Rol de acceso asignado a un usuario: un rol base o un rol personalizado de su iglesia.")
public record RolAsignadoResponse(
		@Schema(description = "Identificador del rol (el que se envía en `rolIds`).", example = "7") Long id,
		@Schema(description = "Nombre del rol.", example = "Secretaría") String nombre,
		@Schema(description = "Rol base: el propio si es base, o del que hereda si es personalizado.",
				example = "ADMIN_IGLESIA") RolBase rolBase) {
}
