package com.iglesia.api.miembros.controller;

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
import com.iglesia.api.miembros.dto.EstadisticasMiembrosResponse;
import com.iglesia.api.miembros.service.EstadisticasMiembrosService;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasMiembrosController {

	private final EstadisticasMiembrosService servicio;

	EstadisticasMiembrosController(EstadisticasMiembrosService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/miembros")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Estadísticas de miembros", description = """
			Miembros activos, altas y bajas del periodo (y del anterior), serie por día, semana o mes, miembros sin grupo \
			o sin líder y distribución por condición, nivel, género y edad. Requiere `miembros.ver`.

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
	EstadisticasMiembrosResponse miembros(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
