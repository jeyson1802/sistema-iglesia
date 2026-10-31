package com.iglesia.api.grupos.controller;

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
import com.iglesia.api.grupos.dto.EstadisticasGruposResponse;
import com.iglesia.api.grupos.service.EstadisticasGruposService;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasGruposController {

	private final EstadisticasGruposService servicio;

	EstadisticasGruposController(EstadisticasGruposService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/grupos")
	@PreAuthorize(Permisos.PUEDE_VER_ESTADISTICAS_GRUPOS)
	@Operation(summary = "Estadísticas de grupos", description = """
			Grupos activos, promedio de integrantes, miembros que están en un grupo, grupos sin reuniones, reuniones \
			del periodo y porcentaje de integrantes que asistieron (con el periodo anterior), y cada grupo de mayor a \
			menor asistencia. Requiere `grupos.ver` o `asistencia.grupo`; para un líder cuenta los grupos que lidera \
			él o alguien de su red.

			""" + DocumentacionEstadisticas.ALCANCE)
	@ApiResponse(responseCode = "200", description = "Cifras del periodo.")
	@ApiResponse(responseCode = "400", description = DocumentacionEstadisticas.PERIODO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO + DocumentacionEstadisticas.SIN_ALCANCE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	EstadisticasGruposResponse grupos(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
