package com.iglesia.api.grupos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cómo llama la iglesia a sus grupos. Si no lo configuró: «Grupo» / «Grupos».")
public record ConfiguracionIglesiaResponse(
		@Schema(description = "Nombre del tipo de grupo en singular.", example = "Hogar de paz") String nombreGrupoSingular,
		@Schema(description = "Nombre del tipo de grupo en plural.", example = "Hogares de paz") String nombreGrupoPlural) {
}
