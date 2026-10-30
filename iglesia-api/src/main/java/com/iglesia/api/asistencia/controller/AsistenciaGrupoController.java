package com.iglesia.api.asistencia.controller;

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

import com.iglesia.api.asistencia.dto.DetalleReunionGrupoResponse;
import com.iglesia.api.asistencia.dto.ReunionGrupoResponse;
import com.iglesia.api.asistencia.service.AsistenciaGrupoService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/grupos/{id:\\d+}/reuniones")
@Tag(name = "Asistencia de grupos", description = """
		Reuniones de un grupo, una por una: cuántos integrantes asistieron y faltaron, y quiénes. A un integrante (miembro \
		activo del grupo) le corresponde una reunión desde su ingreso a la iglesia; si asistió, cuenta siempre. Quien \
		asistió sin ser integrante es visitante. Se usan los integrantes actuales del grupo. Con `asistencia.ver`, \
		cualquier grupo; con `asistencia.grupo`, solo los que lidera (otro responde 404).""")
class AsistenciaGrupoController {

	private static final String DESCRIPCION_ID = "Identificador del grupo.";

	private static final String GRUPO_NO_ENCONTRADO = "El grupo no existe en la iglesia del token o el líder no lo lidera.";

	private final AsistenciaGrupoService servicio;

	AsistenciaGrupoController(AsistenciaGrupoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_HISTORIAL_ASISTENCIA)
	@Operation(summary = "Reuniones de un grupo con su resumen", description = """
			Página de las reuniones del grupo, de la más reciente a la más antigua, con los integrantes esperados, los que \
			asistieron, los que faltaron, los visitantes y el porcentaje de asistencia. Requiere `asistencia.ver` o \
			`asistencia.grupo` (este, en los grupos que lidera).""")
	@ApiResponse(responseCode = "200", description = "Página de reuniones del grupo.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = GRUPO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<ReunionGrupoResponse> reuniones(@Parameter(description = DESCRIPCION_ID, example = "8") @PathVariable Long id,
			@ParameterObject @PageableDefault Pageable paginacion) {
		return servicio.reuniones(id, paginacion);
	}

	@GetMapping("/{reunionId:\\d+}")
	@PreAuthorize(Permisos.PUEDE_VER_HISTORIAL_ASISTENCIA)
	@Operation(summary = "Quiénes asistieron y faltaron a una reunión del grupo", description = """
			Integrantes que asistieron, integrantes que faltaron y visitantes de una reunión del grupo, cada lista por \
			nombre, con su nivel. Requiere `asistencia.ver` o `asistencia.grupo` (este, en los grupos que lidera).""")
	@ApiResponse(responseCode = "200", description = "Detalle de la reunión.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = GRUPO_NO_ENCONTRADO + " También si la reunión no es de este grupo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	DetalleReunionGrupoResponse detalle(@Parameter(description = DESCRIPCION_ID, example = "8") @PathVariable Long id,
			@Parameter(description = "Identificador de la reunión.", example = "120") @PathVariable Long reunionId) {
		return servicio.detalle(id, reunionId);
	}

}
