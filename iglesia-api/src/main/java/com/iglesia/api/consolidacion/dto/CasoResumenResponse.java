package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.model.EstadoCaso;

@Schema(description = "Caso de consolidación para listas (ronda y «Mis asignados»).")
public record CasoResumenResponse(
		@Schema(description = "Identificador del caso.", example = "310") Long id,
		@Schema(description = "Ronda; vacía si es un seguimiento individual.") ReferenciaResponse ronda,
		@Schema(description = "Persona.") PersonaCasoResponse persona,
		@Schema(description = "Consolidador asignado; vacío si aún no tiene.") ReferenciaResponse consolidador,
		@Schema(description = "Estado.", example = "EN_CONTACTO") EstadoCaso estado,
		@Schema(description = "Contactos del caso por medio.") ContactosPorMedioResponse contactos,
		@Schema(description = "Último contacto; vacío si no tiene.") UltimoContactoResponse ultimoContacto,
		@Schema(description = "Próximo contacto previsto.", example = "2026-11-10") LocalDate proximoContacto,
		@Schema(description = "Resultado de la iteración, si está cerrado.") ReferenciaResponse resultado,
		@Schema(description = "Número de iteración de la persona (1 es su primer seguimiento).", example = "2") int iteracion,
		@Schema(description = "Iteración anterior; vacía si es la primera.") IteracionAnteriorResponse anterior) {
}
