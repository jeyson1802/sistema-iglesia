package com.iglesia.api.grupos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Miembro activo que se puede agregar al grupo, con el grupo en el que está ahora.")
public record CandidatoGrupoResponse(
		@Schema(description = "Identificador del miembro.", example = "31") Long id,
		@Schema(description = "Nombres y apellidos.", example = "Rosa Quispe") String nombreCompleto,
		@Schema(description = "Nombre de su nivel.", example = "Miembro", nullable = true) String nivel,
		@Schema(description = "Grupo al que pertenece ahora (su `nombre` es el del líder); agregarlo lo mueve de grupo.",
				nullable = true) ReferenciaResponse grupo) {
}
