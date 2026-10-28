package com.iglesia.seguridad.rol.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.rol.model.RolBase;

@Schema(description = "Rol de acceso: un rol base (global y de solo lectura) o un rol personalizado de la iglesia.")
public record RolResponse(
		@Schema(description = "Identificador del rol (el que se envía en `rolIds` al asignarlo).", example = "7") Long id,
		@Schema(description = "Nombre del rol.", example = "Secretaría") String nombre,
		@Schema(description = "Rol base: el propio si es base, o del que hereda ámbito y página de inicio si es "
				+ "personalizado.", example = "ADMIN_IGLESIA") RolBase rolBase,
		@Schema(description = "`true` si es uno de los 4 roles base, que no se pueden editar ni desactivar.",
				example = "false") boolean base,
		@Schema(description = "Si se puede asignar y si aporta permisos a sus usuarios.", example = "true") boolean activo,
		@Schema(description = "`true` si su rol base es `LIDER` o `MIEMBRO`: solo se puede asignar a un usuario "
				+ "vinculado con un miembro de la iglesia.", example = "false") boolean requiereMiembro,
		@Schema(description = "Códigos de los permisos del rol, ordenados.",
				example = "[\"miembros.crear\", \"miembros.ver\"]") List<String> permisos,
		@Schema(description = "Usuarios que tienen el rol asignado, contando solo los de la iglesia de quien "
				+ "consulta (`ADMIN_SISTEMA`: todos).", example = "3") long cantidadUsuarios) {
}
