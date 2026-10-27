package com.iglesia.seguridad.rol.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nuevo estado de un rol personalizado.")
public record CambiarEstadoRolRequest(
		@Schema(description = "`false` desactiva el rol: deja de aportar permisos en el siguiente refresco del token "
				+ "y ya no se puede asignar. `true` lo reactiva.", example = "false") @NotNull Boolean activo) {
}
