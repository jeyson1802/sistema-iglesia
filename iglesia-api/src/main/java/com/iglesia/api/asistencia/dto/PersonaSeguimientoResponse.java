package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.model.TipoAlerta;

@Schema(description = "Un miembro en la lista de seguimiento, con sus indicadores de asistencia y sus alertas.")
public record PersonaSeguimientoResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Quispe Rojas") String nombreCompleto,
		@Schema(description = "Nombre de su nivel; `null` si no tiene.", example = "Líder") String nivel,
		@Schema(description = "Grupo al que pertenece; `null` si no tiene.", example = "8") Long grupoId,
		@Schema(description = "Nombre de su grupo.", example = "Célula de Juan Pérez") String grupo,
		@Schema(description = "Nombre de su líder; `null` si no tiene.", example = "Juan Pérez Soto") String lider,
		@Schema(description = "Porcentaje de las últimas 8 semanas; `null` sin datos.", example = "40") Integer porcentaje,
		@Schema(description = "Porcentaje de las 8 semanas anteriores; `null` sin datos.", example = "85")
		Integer porcentajeAnterior,
		@Schema(description = "Ocasiones seguidas sin asistir hasta hoy.", example = "4") int faltasSeguidas,
		@Schema(description = "Última fecha en que asistió; `null` si nunca.", example = "2026-10-04") LocalDate ultimaAsistencia,
		@Schema(description = "Alertas, de la más grave a la menos grave.") List<TipoAlerta> alertas) {
}
