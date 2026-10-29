package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.Parameter;

import com.iglesia.api.asistencia.model.TipoAlerta;

/** Parámetros de consulta de la lista de seguimiento. Un filtro omitido no restringe. */
public record FiltroSeguimiento(
		@Parameter(description = "Solo quienes tienen esta alerta.", example = "FALTAS_SEGUIDAS") TipoAlerta alerta,
		@Parameter(description = "Solo los integrantes de este grupo.", example = "8") Long grupoId,
		@Parameter(description = "Parte del nombre o apellido.", example = "quispe") String q,
		@Parameter(description = "Incluye también a quienes no tienen alertas (por defecto, solo los que tienen).",
				example = "false") Boolean todos) {

	public boolean incluirSinAlertas() {
		return Boolean.TRUE.equals(todos);
	}

}
