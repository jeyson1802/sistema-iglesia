package com.iglesia.api.consolidacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Resumen de consolidación de una persona para su ficha.")
public record ConsolidacionMiembroResponse(
		@Schema(description = "Iteraciones (casos) que tuvo.", example = "3") int iteraciones,
		@Schema(description = "Todos sus contactos por medio.") ContactosPorMedioResponse contactos,
		@Schema(description = "Resultado de su última iteración cerrada.") ReferenciaResponse ultimoResultado,
		@Schema(description = "Cuándo se cerró.", example = "2026-10-12T15:00:00Z") Instant ultimoResultadoEn,
		@Schema(description = "Seguimiento abierto, si lo tiene.") CasoAbiertoResponse casoAbierto,
		@Schema(description = "Fechas con contacto en las últimas 12 semanas (para marcarlas en su asistencia).") List<LocalDate> fechasContactoRecientes,
		@Schema(description = "Si el usuario del token puede iniciarle un seguimiento individual ahora.", example = "true") boolean puedeIniciar) {
}
