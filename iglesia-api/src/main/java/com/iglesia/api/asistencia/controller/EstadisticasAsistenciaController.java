package com.iglesia.api.asistencia.controller;

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

import com.iglesia.api.asistencia.dto.EstadisticasAsistenciaResponse;
import com.iglesia.api.asistencia.service.EstadisticasAsistenciaService;
import com.iglesia.api.common.estadisticas.constant.DocumentacionEstadisticas;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasAsistenciaController {

	private final EstadisticasAsistenciaService servicio;

	EstadisticasAsistenciaController(EstadisticasAsistenciaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/asistencia")
	@PreAuthorize(Permisos.PUEDE_VER_ESTADISTICAS_ASISTENCIA)
	@Operation(summary = "Estadísticas de asistencia", description = """
			Cultos registrados y asistentes promedio por culto (con el periodo anterior y por día, semana o mes), personas \
			distintas que asistieron, personas con alertas de seguimiento y personas que llegaron en el periodo y \
			volvieron. Requiere `asistencia.ver` o `asistencia.grupo`; en la red solo cuentan los asistentes de ella.

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
	EstadisticasAsistenciaResponse asistencia(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
