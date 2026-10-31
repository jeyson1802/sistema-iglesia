package com.iglesia.api.asistencia.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = """
		Cifras de asistencia de la iglesia o, para un líder, de su red: cultos y asistentes, personas que asistieron, \
		alertas de seguimiento y personas nuevas que volvieron. Solo cifras.""")
public record EstadisticasAsistenciaResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "`true` si las cifras son solo de la red del líder.", example = "false") boolean red,
		@Schema(description = "Cultos con reunión registrada en el periodo.", example = "9") long cultos,
		@Schema(description = "Cultos con reunión registrada en el periodo anterior.", example = "8") long cultosAnterior,
		@Schema(description = "Asistentes promedio por culto (del alcance); nulo si no hubo cultos.", example = "86.5",
				nullable = true) Double asistentesPromedio,
		@Schema(description = "Asistentes promedio por culto en el periodo anterior.", example = "81.0",
				nullable = true) Double asistentesPromedioAnterior,
		@Schema(description = "Personas distintas que asistieron a alguna reunión en el periodo.",
				example = "142") long personasQueAsistieron,
		@Schema(description = "Personas distintas que asistieron en el periodo anterior.",
				example = "130") long personasQueAsistieronAnterior,
		@Schema(description = "Cultos y asistentes promedio por día, semana o mes, sin huecos.",
				example = "[{\"inicio\": \"2026-10-05\", \"cultos\": 2, \"asistentesPromedio\": 86.5}]")
		List<PuntoCultosResponse> serie,
		@Schema(description = "Personas con alertas de seguimiento a hoy.", example = "{\"conAlertas\": 23, "
				+ "\"faltasSeguidas\": 14, \"nuevoSinVolver\": 5, \"asistenciaEnCaida\": 9}") ConteoAlertasResponse alertas,
		@Schema(description = "Personas que llegaron en el periodo.", example = "12") long llegaron,
		@Schema(description = "De las que llegaron, cuántas asistieron a alguna reunión después del día en que "
				+ "llegaron.", example = "7") long volvieron) {

	public EstadisticasAsistenciaResponse {
		serie = List.copyOf(serie);
	}

}
