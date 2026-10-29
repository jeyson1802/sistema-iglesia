package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Size;

import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.model.CondicionMiembro;

/** Parámetros de consulta opcionales de {@code GET /api/miembros}. Un valor nulo o vacío no filtra. */
public record FiltroMiembros(
		@Parameter(description = "Parte de los nombres y apellidos, en cualquier orden y sin distinguir mayúsculas.",
				example = "perez") @Size(max = LimitesMiembro.BUSQUEDA_MAX) String q,
		@Parameter(description = "Condición del miembro.", example = "NUEVO") CondicionMiembro condicion,
		@Parameter(description = "Solo los miembros con este nivel.", example = "3") Long nivelId,
		@Parameter(description = "Solo los miembros de este grupo.", example = "4") Long grupoId,
		@Parameter(description = "Solo los liderados directamente por este miembro.", example = "12") Long liderId,
		@Parameter(description = "`true`: solo activos; `false`: solo inactivos. Sin valor: todos.",
				example = "true") Boolean activo) {
}
