package com.iglesia.api.consolidacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.dto.ConsolidacionMiembroResponse;
import com.iglesia.api.consolidacion.dto.IteracionResponse;
import com.iglesia.api.consolidacion.service.HistorialConsolidacionService;

@RestController
@RequestMapping("/api/miembros/{id}/consolidacion")
@Tag(name = "Consolidación: historial de la persona", description = """
		Historia de consolidación de una persona: resumen para su ficha e iteraciones con sus contactos. Se ve con \
		`consolidacion.ver` o `consolidacion.administrar`; con `consolidacion.contactar`, solo de las personas que \
		tiene en un caso abierto o de su red (si no, 404).""")
class ConsolidacionMiembroController {

	private static final String DESCRIPCION_ID = "Identificador del miembro.";

	private static final String NO_ENCONTRADO = "El miembro no existe o está fuera del ámbito del usuario.";

	private final HistorialConsolidacionService servicio;

	ConsolidacionMiembroController(HistorialConsolidacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONSOLIDACION)
	@Operation(summary = "Resumen de consolidación de una persona", description = """
			Iteraciones, contactos por medio, último resultado, seguimiento abierto, días con contacto de las últimas 12 \
			semanas (para marcarlos en su asistencia) y si el usuario puede iniciarle un seguimiento.""")
	@ApiResponse(responseCode = "200", description = "Resumen de consolidación.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConsolidacionMiembroResponse resumen(@Parameter(description = DESCRIPCION_ID, example = "40") @PathVariable Long id) {
		return servicio.resumen(id);
	}

	@GetMapping("/historial")
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONSOLIDACION)
	@Operation(summary = "Historial de consolidación de una persona", description = """
			Iteraciones de la más reciente a la más antigua, paginadas, cada una con su ronda, consolidador, contactos, \
			resultado y observación de cierre.""")
	@ApiResponse(responseCode = "200", description = "Página de iteraciones.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<IteracionResponse> historial(@Parameter(description = DESCRIPCION_ID, example = "40") @PathVariable Long id,
			@ParameterObject @PageableDefault Pageable paginacion) {
		return servicio.historial(id, paginacion);
	}

}
