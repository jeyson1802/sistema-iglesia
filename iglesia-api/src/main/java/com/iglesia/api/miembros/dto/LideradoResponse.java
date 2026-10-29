package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona que un miembro lidera directamente, para el árbol de liderazgo de la ficha.")
public record LideradoResponse(
		@Schema(description = "Identificador del liderado.", example = "31") Long id,
		@Schema(description = "Nombres y apellidos.", example = "Rosa Quispe") String nombreCompleto,
		@Schema(description = "Nombre de su nivel.", example = "Líder", nullable = true) String nivel,
		@Schema(description = "Cantidad de miembros activos que lidera directamente: si es mayor que cero, su nodo se "
				+ "puede expandir.", example = "3") long cantidadLiderados) {
}
