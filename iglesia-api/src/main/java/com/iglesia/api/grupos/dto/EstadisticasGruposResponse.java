package com.iglesia.api.grupos.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = """
		Cifras de los grupos activos de la iglesia o, para un líder, de los que lidera él o alguien de su red, en el \
		periodo y en el anterior.""")
public record EstadisticasGruposResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "`true` si son solo los grupos de la red del líder.", example = "false") boolean red,
		@Schema(description = "Grupos activos.", example = "14") long grupos,
		@Schema(description = "Promedio de integrantes por grupo.", example = "9.5") double integrantesPromedio,
		@Schema(description = "Miembros activos del alcance que están en un grupo.", example = "133") long miembrosEnGrupo,
		@Schema(description = "Miembros activos del alcance.", example = "180") long miembrosActivos,
		@Schema(description = "Grupos sin ninguna reunión registrada en el periodo.", example = "3") long sinReuniones,
		@Schema(description = "Reuniones de grupo registradas en el periodo.", example = "41") long reuniones,
		@Schema(description = "Reuniones de grupo registradas en el periodo anterior.",
				example = "38") long reunionesAnterior,
		@Schema(description = "Porcentaje de integrantes que asistieron a las reuniones de su grupo; nulo sin reuniones.",
				example = "72", nullable = true) Integer porcentajeAsistencia,
		@Schema(description = "El mismo porcentaje en el periodo anterior.", example = "68",
				nullable = true) Integer porcentajeAsistenciaAnterior,
		@Schema(description = "Cada grupo, de mayor a menor asistencia (los que no tuvieron reuniones al final).",
				example = "[{\"grupoId\": 22, \"nombre\": \"Célula de Ana Mendoza Silva\", \"integrantes\": 12, "
						+ "\"reuniones\": 4, \"porcentaje\": 75}]") List<GrupoEstadisticaResponse> porGrupo) {

	public EstadisticasGruposResponse {
		porGrupo = List.copyOf(porGrupo);
	}

}
