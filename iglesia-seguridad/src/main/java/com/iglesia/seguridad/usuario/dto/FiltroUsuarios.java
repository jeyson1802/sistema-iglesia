package com.iglesia.seguridad.usuario.dto;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;

/** Parámetros de consulta opcionales de {@code GET /api/usuarios}. Un valor nulo o vacío no filtra. */
public record FiltroUsuarios(
		@Parameter(description = "Parte del correo, sin distinguir mayúsculas.", example = "prueba")
		@Size(max = LimitesUsuario.CORREO_MAX) String correo,
		@Parameter(description = "`true`: solo activos; `false`: solo inactivos. Sin valor: todos.",
				example = "true") Boolean activo,
		@Parameter(description = "Solo los usuarios que tienen asignado este rol.", example = "7") Long rolId) {
}
