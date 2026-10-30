package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidades de «Mis asignados» y avance del consolidador.")
public record ResumenMisCasosResponse(
		@Schema(description = "Casos con el próximo contacto hoy o vencido.", example = "4") long paraHoy,
		@Schema(description = "Casos sin contactos.", example = "5") long sinContactar,
		@Schema(description = "Casos con contactos.", example = "6") long enContacto,
		@Schema(description = "Casos asignados en rondas en curso e individuales (abiertos y cerrados de rondas en curso).", example = "15") long asignados,
		@Schema(description = "De ellos, en cuántos ya habló con la persona.", example = "9") long contactados) {
}
