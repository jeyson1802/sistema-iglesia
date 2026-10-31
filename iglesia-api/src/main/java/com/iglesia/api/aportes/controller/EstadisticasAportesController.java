package com.iglesia.api.aportes.controller;

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

import com.iglesia.api.aportes.dto.EstadisticasAportesResponse;
import com.iglesia.api.aportes.service.EstadisticasAportesService;
import com.iglesia.api.common.estadisticas.constant.DocumentacionEstadisticas;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasAportesController {

	private final EstadisticasAportesService servicio;

	EstadisticasAportesController(EstadisticasAportesService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/aportes")
	@PreAuthorize(Permisos.PUEDE_VER_APORTES)
	@Operation(summary = "Estadísticas de aportes", description = """
			Total, cantidad y desglose por categoría y por medio de los aportes vigentes del periodo (nunca cuenta los \
			anulados), el total del periodo anterior y la serie por día, semana o mes, en la moneda de la iglesia. \
			Requiere `aportes.ver` y alcance de toda la iglesia (`ADMIN_IGLESIA`, o `ADMIN_SISTEMA` dentro de una \
			iglesia): un líder con `aportes.ver` solo para sus aportes recibe 403. Es el único endpoint de \
			estadísticas que devuelve datos de aportes.""")
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
	EstadisticasAportesResponse aportes(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
