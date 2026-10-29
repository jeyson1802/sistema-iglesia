package com.iglesia.api.grupos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro activo que pertenece al grupo.")
public record IntegranteGrupoResponse(
		@Schema(description = "Identificador del miembro.", example = "31") Long id,
		@Schema(description = "Nombres y apellidos.", example = "Rosa Quispe") String nombreCompleto,
		@Schema(description = "Nombre de su nivel.", example = "Miembro", nullable = true) String nivel) {
}
