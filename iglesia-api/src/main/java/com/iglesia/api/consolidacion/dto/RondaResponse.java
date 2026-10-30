package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.consolidacion.model.EstadoRonda;

@Schema(description = "Ronda de consolidación con el resumen de su avance.")
public record RondaResponse(
		@Schema(description = "Identificador.", example = "7") Long id,
		@Schema(description = "Nombre.", example = "Consolidación noviembre 2026") String nombre,
		@Schema(description = "Descripción.", example = "Nuevos que llegaron en octubre") String descripcion,
		@Schema(description = "Fecha de inicio.", example = "2026-11-02") LocalDate fechaInicio,
		@Schema(description = "Fecha de fin.", example = "2026-11-30") LocalDate fechaFin,
		@Schema(description = "Estado.", example = "EN_CURSO") EstadoRonda estado,
		@Schema(description = "Personas en el lote.", example = "40") long personas,
		@Schema(description = "Casos sin contactos.", example = "12") long pendientes,
		@Schema(description = "Casos con al menos un contacto.", example = "18") long enContacto,
		@Schema(description = "Casos cerrados (con o sin resultado).", example = "10") long cerrados,
		@Schema(description = "Casos cerrados sin resultado al cerrar la ronda.", example = "0") long noConcluidos,
		@Schema(description = "Casos abiertos sin consolidador.", example = "3") long sinAsignar,
		@Schema(description = "Casos abiertos con el próximo contacto vencido.", example = "5") long vencidos) {
}
