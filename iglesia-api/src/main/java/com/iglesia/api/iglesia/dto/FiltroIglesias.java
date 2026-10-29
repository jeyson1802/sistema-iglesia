package com.iglesia.api.iglesia.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

import com.iglesia.api.iglesia.constant.LimitesIglesia;

/** Parámetros de consulta opcionales de {@code GET /api/iglesias}. Un valor nulo o vacío no filtra. */
public record FiltroIglesias(
		@Parameter(description = "Parte del nombre comercial o de la denominación, sin distinguir mayúsculas ni "
				+ "tildes.", example = "lima") @Size(max = LimitesIglesia.BUSQUEDA_MAX) String q,
		@Parameter(description = "Código INEI de 2, 4 o 6 dígitos: solo las iglesias de ese departamento, provincia "
				+ "o distrito.", example = "1501") @Pattern(regexp = LimitesIglesia.UBIGEO_PATRON,
						message = "debe ser un código de departamento (2 dígitos), provincia (4) o distrito (6)") String ubigeo,
		@Parameter(description = "`true`: solo activas; `false`: solo inactivas. Sin valor: todas.",
				example = "true") Boolean activa) {
}
