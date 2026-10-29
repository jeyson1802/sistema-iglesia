package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.TipoAlerta;

@Schema(description = """
		Seguimiento de la asistencia de un miembro. Le corresponden los días de culto (todos los cultos de una misma \
		fecha cuentan como uno) y las reuniones de su grupo actual, desde su fecha de ingreso; los eventos no cuentan \
		como falta. Las semanas van de lunes a domingo y la actual está incluida.""")
public record AsistenciaMiembroResponse(
		@Schema(description = "Ocasiones que le correspondían en las últimas 8 semanas.", example = "16") long reunionesQueCuentan,
		@Schema(description = "A cuántas de ellas asistió.", example = "12") long asistidas,
		@Schema(description = "Porcentaje de las últimas 8 semanas, de 0 a 100; `null` si no le correspondía ninguna.",
				example = "75") Integer porcentaje,
		@Schema(description = "Porcentaje de las 8 semanas anteriores, para la tendencia; `null` sin datos.", example = "60")
		Integer porcentajeAnterior,
		@Schema(description = "Ocasiones seguidas sin asistir hasta hoy.", example = "0") int faltasSeguidas,
		@Schema(description = "Última fecha en que asistió a cualquier reunión; `null` si nunca.", example = "2026-10-25")
		LocalDate ultimaAsistencia,
		@Schema(description = "Alertas que tiene, de la más grave a la menos grave.") List<TipoAlerta> alertas,
		@Schema(description = "Evolución de las últimas 12 semanas, de la más antigua a la actual.")
		List<SemanaAsistenciaResponse> semanas,
		@Schema(description = "Últimas reuniones a las que asistió (de cualquier tipo), la más reciente primero.")
		List<ReunionAsistidaResponse> ultimas) {
}
