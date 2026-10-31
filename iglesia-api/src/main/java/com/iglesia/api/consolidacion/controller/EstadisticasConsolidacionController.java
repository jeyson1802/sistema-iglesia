package com.iglesia.api.consolidacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.common.estadisticas.constant.DocumentacionEstadisticas;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.dto.EstadisticasConsolidacionResponse;
import com.iglesia.api.consolidacion.service.EstadisticasConsolidacionService;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasConsolidacionController {

	private final EstadisticasConsolidacionService servicio;

	EstadisticasConsolidacionController(EstadisticasConsolidacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/consolidacion")
	@PreAuthorize(Permisos.PUEDE_VER_ESTADISTICAS_CONSOLIDACION)
	@Operation(summary = "Estadísticas de consolidación", description = """
			Rondas en curso, casos abiertos, casos cerrados y contactos del periodo (con el periodo anterior), \
			personas contactadas que volvieron a asistir, contactos por medio y cierres por resultado. Requiere \
			`consolidacion.ver` o `consolidacion.administrar`. Siempre de toda la iglesia: un líder no tiene estas \
			cifras (403) y ve sus asignados en `GET /api/consolidacion/mis-casos/resumen`.""")
	@ApiResponse(responseCode = "200", description = "Cifras del periodo.")
	@ApiResponse(responseCode = "400", description = DocumentacionEstadisticas.PERIODO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO + DocumentacionEstadisticas.SIN_ALCANCE
			+ " También para un líder: estas cifras son solo de toda la iglesia.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	EstadisticasConsolidacionResponse consolidacion(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
