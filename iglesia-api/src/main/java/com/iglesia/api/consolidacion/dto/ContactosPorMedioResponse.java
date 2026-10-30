package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contactos de un caso o de una persona por medio.")
public record ContactosPorMedioResponse(
		@Schema(description = "Total de contactos.", example = "4") long total,
		@Schema(description = "Llamadas.", example = "3") long llamadas,
		@Schema(description = "Mensajes de WhatsApp.", example = "0") long whatsapp,
		@Schema(description = "Visitas.", example = "1") long visitas,
		@Schema(description = "Conversaciones en la iglesia.", example = "0") long enLaIglesia,
		@Schema(description = "Contactos en que se habló con la persona.", example = "1") long conRespuesta) {
}
