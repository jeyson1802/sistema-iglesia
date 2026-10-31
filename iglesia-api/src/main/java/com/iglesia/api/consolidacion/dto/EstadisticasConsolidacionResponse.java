package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = "Cifras de la consolidación de toda la iglesia en el periodo y en el anterior. Solo cifras.")
public record EstadisticasConsolidacionResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "Rondas en curso a hoy.", example = "2") long rondasEnCurso,
		@Schema(description = "Casos abiertos (pendientes o en contacto) a hoy.", example = "37") long casosAbiertos,
		@Schema(description = "Casos cerrados en el periodo.", example = "15") long cerrados,
		@Schema(description = "Casos cerrados en el periodo anterior.", example = "11") long cerradosAnterior,
		@Schema(description = "Contactos registrados en el periodo.", example = "64") long contactos,
		@Schema(description = "Contactos registrados en el periodo anterior.", example = "58") long contactosAnterior,
		@Schema(description = "Personas distintas contactadas en el periodo.", example = "40") long contactadas,
		@Schema(description = "De las contactadas, cuántas asistieron a alguna reunión después de su primer contacto "
				+ "del periodo.", example = "17") long volvieron,
		@Schema(description = "Contactos del periodo por medio.",
				example = "[{\"clave\": \"LLAMADA\", \"etiqueta\": \"Llamada\", \"valor\": 30}]") List<ConteoResponse> contactosPorMedio,
		@Schema(description = "Casos cerrados en el periodo por resultado, en el orden de los resultados.",
				example = "[{\"clave\": \"Se integró\", \"etiqueta\": \"Se integró\", \"valor\": 9}]")
		List<ConteoResponse> cierresPorResultado) {

	public EstadisticasConsolidacionResponse {
		contactosPorMedio = List.copyOf(contactosPorMedio);
		cierresPorResultado = List.copyOf(cierresPorResultado);
	}

}
