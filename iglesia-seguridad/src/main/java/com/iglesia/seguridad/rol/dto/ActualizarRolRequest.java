package com.iglesia.seguridad.rol.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.rol.constant.LimitesRol;

/** El rol base no cambia: cambiarlo alteraría la página de inicio y el ámbito de todos sus usuarios. */
@Schema(description = "Cambios de un rol personalizado. El rol base no se modifica.")
public record ActualizarRolRequest(
		@Schema(description = "Nombre del rol; único en la iglesia sin distinguir mayúsculas.",
				example = "Secretaría general") @NotBlank @Size(max = LimitesRol.NOMBRE_MAX) String nombre,
		@Schema(description = "Códigos de los permisos; deben ser un subconjunto de los del rol base. Reemplazan a "
				+ "los actuales.", example = "[\"miembros.ver\", \"miembros.crear\", \"miembros.editar\"]")
		@NotNull Set<@NotBlank String> permisos) {
}
