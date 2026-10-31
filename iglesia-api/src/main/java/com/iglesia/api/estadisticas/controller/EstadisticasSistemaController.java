package com.iglesia.api.estadisticas.controller;

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
import com.iglesia.api.estadisticas.dto.EstadisticasSistemaResponse;
import com.iglesia.api.estadisticas.service.EstadisticasSistemaService;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = DocumentacionEstadisticas.TAG, description = DocumentacionEstadisticas.TAG_DESCRIPCION)
class EstadisticasSistemaController {

	private final EstadisticasSistemaService servicio;

	EstadisticasSistemaController(EstadisticasSistemaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/sistema")
	@PreAuthorize(Permisos.PUEDE_VER_ESTADISTICAS_SISTEMA)
	@Operation(summary = "Estadísticas de todas las iglesias", description = """
			Crecimiento de iglesias (total, altas, iglesias con actividad, por tamaño y las más grandes), crecimiento \
			de miembros de todas las iglesias (activos, altas, bajas y por condición) y actividad registrada por \
			módulo (miembros, asistencias y contactos de consolidación) por día, semana o mes, comparados con el periodo \
			anterior. Solo agregados: nunca datos de personas ni de aportes. Las iglesias sí se nombran (catálogo).

			Requiere `estadisticas.sistema` (solo `ADMIN_SISTEMA`) y la vista global: dentro de una iglesia \
			responde 403. El uso del sistema (usuarios activos) lo da iglesia-seguridad en \
			`GET /api/estadisticas/uso`.""")
	@ApiResponse(responseCode = "200", description = "Cifras del periodo.")
	@ApiResponse(responseCode = "400", description = DocumentacionEstadisticas.PERIODO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También si el administrador del sistema está dentro de una iglesia.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	EstadisticasSistemaResponse sistema(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
