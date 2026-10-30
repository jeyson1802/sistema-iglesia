package com.iglesia.api.consolidacion.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.model.EstadoCaso;

@Schema(description = "Una iteración (caso) de consolidación de una persona, con sus contactos.")
public record IteracionResponse(
		@Schema(description = "Caso.", example = "205") Long casoId,
		@Schema(description = "Ronda; vacía si fue un seguimiento individual.") ReferenciaResponse ronda,
		@Schema(description = "Estado.", example = "CERRADO") EstadoCaso estado,
		@Schema(description = "Cuándo empezó.", example = "2026-08-01T13:00:00Z") Instant desde,
		@Schema(description = "Cuándo se cerró.", example = "2026-08-20T15:00:00Z") Instant cerradoEn,
		@Schema(description = "Quién la consolidó.") ReferenciaResponse consolidador,
		@Schema(description = "Contactos por medio.") ContactosPorMedioResponse contactos,
		@Schema(description = "Resultado; vacío si no concluyó o sigue abierta.") ReferenciaResponse resultado,
		@Schema(description = "Observación final.", example = "Dijo que por ahora no puede venir") String observacionCierre,
		@Schema(description = "Contactos, del más reciente al más antiguo.") List<ContactoResponse> detalle) {
}
