package com.iglesia.seguridad.actividad.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.common.periodo.dto.PeriodoResponse;
import com.iglesia.seguridad.common.periodo.dto.PuntoSerieResponse;

@Schema(description = """
		Uso del sistema en el periodo: usuarios que entraron (iniciaron sesión o renovaron su sesión al menos una vez \
		ese día). Solo cifras: nunca nombres ni correos. Para todas las iglesias o solo para la del token.""")
public record EstadisticasUsoResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "`true` si cuenta todas las iglesias (administrador del sistema en la vista global).",
				example = "false") boolean global,
		@Schema(description = "Usuarios activos (cuentas habilitadas) a hoy.", example = "48") long usuarios,
		@Schema(description = "Usuarios creados en el periodo.", example = "5") long usuariosNuevos,
		@Schema(description = "Usuarios distintos que entraron en el periodo.", example = "31") long activosPeriodo,
		@Schema(description = "Usuarios distintos que entraron en el periodo anterior.",
				example = "27") long activosPeriodoAnterior,
		@Schema(description = "Promedio de usuarios distintos por día en el periodo.",
				example = "6.4") double activosDiarios,
		@Schema(description = "Promedio de usuarios distintos por día en el periodo anterior.",
				example = "5.9") double activosDiariosAnterior,
		@Schema(description = "Usuarios distintos en los últimos 7 días (hasta hoy).", example = "18") long activosSemanales,
		@Schema(description = "Usuarios distintos en los últimos 30 días (hasta hoy).",
				example = "29") long activosMensuales,
		@Schema(description = "Iglesias con al menos un usuario activo en el periodo; solo en la vista global.",
				example = "7", nullable = true) Long iglesiasActivas,
		@Schema(description = "Iglesias con al menos un usuario activo en el periodo anterior; solo en la vista "
				+ "global.", example = "6", nullable = true) Long iglesiasActivasAnterior,
		@Schema(description = "Usuarios distintos por día, semana o mes, sin huecos.",
				example = "[{\"inicio\": \"2026-10-05\", \"valor\": 14}, {\"inicio\": \"2026-10-12\", \"valor\": 17}]") List<PuntoSerieResponse> serie,
		@Schema(description = "Usuarios distintos del periodo por rol base, de mayor a menor alcance.",
				example = "[{\"clave\": \"ADMIN_IGLESIA\", \"valor\": 2}, {\"clave\": \"LIDER\", \"valor\": 9}]")
		List<ConteoResponse> porRolBase) {

	public EstadisticasUsoResponse {
		serie = List.copyOf(serie);
		porRolBase = List.copyOf(porRolBase);
	}

}
