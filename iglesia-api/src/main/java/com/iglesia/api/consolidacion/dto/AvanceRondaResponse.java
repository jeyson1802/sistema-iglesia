package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores de una ronda.")
public record AvanceRondaResponse(
		@Schema(description = "Personas en el lote.", example = "40") long personas,
		@Schema(description = "Personas con las que se habló al menos una vez.", example = "26") long contactadas,
		@Schema(description = "Casos sin contactos.", example = "12") long pendientes,
		@Schema(description = "Casos con contactos, aún abiertos.", example = "18") long enContacto,
		@Schema(description = "Casos cerrados.", example = "10") long cerrados,
		@Schema(description = "Casos cerrados sin resultado.", example = "0") long noConcluidos,
		@Schema(description = "Contactadas que asistieron en los 28 días siguientes a su primera conversación.", example = "11") long volvieronAAsistir,
		@Schema(description = "Horas promedio desde que la persona entró en la ronda hasta su primer contacto.", example = "30.5") Double horasHastaPrimerContacto,
		@Schema(description = "Cerrados por resultado, en el orden del catálogo.") List<ConteoResultadoResponse> porResultado,
		@Schema(description = "Avance de cada consolidador.") List<AvanceConsolidadorResponse> porConsolidador) {
}
