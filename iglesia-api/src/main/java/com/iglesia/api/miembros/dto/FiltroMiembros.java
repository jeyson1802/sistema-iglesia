package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.model.CondicionMiembro;

/** Parámetros de consulta opcionales de {@code GET /api/miembros}. Un valor nulo o vacío no filtra. */
public record FiltroMiembros(
		@Parameter(description = "Parte de los nombres y apellidos, en cualquier orden y sin distinguir mayúsculas.",
				example = "perez") @Size(max = LimitesMiembro.BUSQUEDA_MAX) String q,
		@Parameter(description = "Condición del miembro.", example = "NUEVO") CondicionMiembro condicion,
		@Parameter(description = "Solo los miembros con este nivel.", example = "3") Long nivelId,
		@Parameter(description = "Solo los liderados directamente por este miembro.", example = "12") Long liderId,
		@Parameter(description = "Código INEI de 2, 4 o 6 dígitos: solo los que viven en ese departamento, provincia "
				+ "o distrito.", example = "1501") @Pattern(regexp = LimitesMiembro.UBIGEO_PATRON,
						message = "debe ser un código de departamento (2 dígitos), provincia (4) o distrito (6)") String ubigeo,
		@Parameter(description = "`true`: solo activos; `false`: solo inactivos. Sin valor: todos.",
				example = "true") Boolean activo) {
}
