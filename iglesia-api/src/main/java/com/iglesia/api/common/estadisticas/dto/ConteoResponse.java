package com.iglesia.api.common.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidad de una categoría de una distribución (p. ej. miembros por condición).")
public record ConteoResponse(
		@Schema(description = "Código de la categoría.", example = "NUEVO") String clave,
		@Schema(description = "Nombre de la categoría para mostrar.", example = "Nuevo") String etiqueta,
		@Schema(description = "Cantidad.", example = "18") long valor) {
}
