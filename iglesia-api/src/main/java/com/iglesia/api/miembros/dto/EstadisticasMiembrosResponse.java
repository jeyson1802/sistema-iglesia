package com.iglesia.api.miembros.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = """
		Cifras de los miembros de la iglesia o, para un líder, de su red (las personas que lidera directa e \
		indirectamente). Solo conteos: nunca nombres ni datos personales.""")
public record EstadisticasMiembrosResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "`true` si las cifras son solo de la red del líder.", example = "false") boolean red,
		@Schema(description = "Miembros activos a hoy.", example = "245") long activos,
		@Schema(description = "Personas que lidera directamente (solo en la red).", example = "6",
				nullable = true) Long directos,
		@Schema(description = "Altas del periodo (por fecha de ingreso o de registro).", example = "12") long altas,
		@Schema(description = "Altas del periodo anterior.", example = "9") long altasAnterior,
		@Schema(description = "Miembros desactivados en el periodo.", example = "2") long bajas,
		@Schema(description = "Miembros desactivados en el periodo anterior.", example = "3") long bajasAnterior,
		@Schema(description = "Miembros activos que no están en ningún grupo.", example = "31") long sinGrupo,
		@Schema(description = "Miembros activos sin líder, sin contar al pastor principal (solo en toda la iglesia).",
				example = "4", nullable = true) Long sinLider,
		@Schema(description = "Altas y bajas por día, semana o mes, sin huecos.",
				example = "[{\"inicio\": \"2026-10-05\", \"altas\": 4, \"bajas\": 1}]") List<PuntoAltasBajasResponse> serie,
		@Schema(description = "Miembros activos por condición.",
				example = "[{\"clave\": \"NUEVO\", \"etiqueta\": \"Nuevo\", \"valor\": 18}]") List<ConteoResponse> porCondicion,
		@Schema(description = "Miembros activos por nivel, en el orden de los niveles.",
				example = "[{\"clave\": \"3\", \"etiqueta\": \"Líder\", \"valor\": 22}]") List<ConteoResponse> porNivel,
		@Schema(description = "Miembros activos por género.",
				example = "[{\"clave\": \"FEMENINO\", \"etiqueta\": \"Mujeres\", \"valor\": 130}]") List<ConteoResponse> porGenero,
		@Schema(description = "Miembros activos por rango de edad.",
				example = "[{\"clave\": \"JOVENES\", \"etiqueta\": \"18 a 29\", \"valor\": 54}]") List<ConteoResponse> porEdad) {

	public EstadisticasMiembrosResponse {
		serie = List.copyOf(serie);
		porCondicion = List.copyOf(porCondicion);
		porNivel = List.copyOf(porNivel);
		porGenero = List.copyOf(porGenero);
		porEdad = List.copyOf(porEdad);
	}

}
