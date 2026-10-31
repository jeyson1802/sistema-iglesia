package com.iglesia.api.estadisticas.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = """
		Cifras de todas las iglesias para el administrador del sistema: crecimiento de iglesias y de miembros y \
		actividad registrada por módulo. Solo agregados: nunca datos de personas ni de aportes.""")
public record EstadisticasSistemaResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"ANIO\", "
				+ "\"desde\": \"2026-01-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2025-03-04\", "
				+ "\"anteriorHasta\": \"2025-12-31\", \"granularidad\": \"MES\"}") PeriodoResponse periodo,
		@Schema(description = "Iglesias activas en el catálogo.", example = "18") long iglesias,
		@Schema(description = "Iglesias dadas de alta en el periodo.", example = "3") long iglesiasNuevas,
		@Schema(description = "Iglesias dadas de alta en el periodo anterior.", example = "2") long iglesiasNuevasAnterior,
		@Schema(description = "Iglesias que registraron miembros, asistencias o contactos en el periodo.",
				example = "15") long iglesiasConActividad,
		@Schema(description = "Iglesias con actividad en el periodo anterior.",
				example = "13") long iglesiasConActividadAnterior,
		@Schema(description = "Miembros activos de todas las iglesias.", example = "5320") long miembros,
		@Schema(description = "Miembros que llegaron en el periodo.", example = "640") long altas,
		@Schema(description = "Miembros que llegaron en el periodo anterior.", example = "590") long altasAnterior,
		@Schema(description = "Miembros desactivados en el periodo.", example = "48") long bajas,
		@Schema(description = "Miembros desactivados en el periodo anterior.", example = "51") long bajasAnterior,
		@Schema(description = "Crecimiento y actividad por día, semana o mes, sin huecos.",
				example = "[{\"inicio\": \"2026-10-01\", \"iglesiasNuevas\": 1, \"altas\": 85, \"bajas\": 6, "
						+ "\"miembrosRegistrados\": 90, \"asistenciasMarcadas\": 2140, \"contactosConsolidacion\": 310}]")
		List<PuntoSistemaResponse> serie,
		@Schema(description = "Miembros activos de todas las iglesias por condición.",
				example = "[{\"clave\": \"NUEVO\", \"etiqueta\": \"Nuevo\", \"valor\": 820}]") List<ConteoResponse> porCondicion,
		@Schema(description = "Iglesias activas por cantidad de miembros activos, de menor a mayor.",
				example = "[{\"clave\": \"HASTA_50\", \"etiqueta\": \"1 a 50\", \"valor\": 7}]") List<ConteoResponse> porTamano,
		@Schema(description = "Las iglesias activas con más miembros activos.",
				example = "[{\"iglesiaId\": 28, \"nombre\": \"Iglesia Vida Nueva\", \"miembros\": 412}]")
		List<IglesiaMiembrosResponse> mayores) {

	public EstadisticasSistemaResponse {
		serie = List.copyOf(serie);
		porCondicion = List.copyOf(porCondicion);
		porTamano = List.copyOf(porTamano);
		mayores = List.copyOf(mayores);
	}

}
