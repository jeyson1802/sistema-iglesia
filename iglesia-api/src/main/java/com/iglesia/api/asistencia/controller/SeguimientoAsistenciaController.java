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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.asistencia.dto.ConteoAlertasResponse;
import com.iglesia.api.asistencia.dto.FiltroSeguimiento;
import com.iglesia.api.asistencia.dto.PersonaSeguimientoResponse;
import com.iglesia.api.asistencia.dto.SeguimientoGrupoResponse;
import com.iglesia.api.asistencia.dto.ServiciosSeguimientoResponse;
import com.iglesia.api.asistencia.service.SeguimientoAsistenciaService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api")
@Tag(name = "Seguimiento de asistencia", description = """
		Quién está faltando y cómo evoluciona la asistencia, por persona, por grupo y por servicio. A cada miembro activo \
		le corresponden, desde su ingreso, los días de culto (todos los cultos de una fecha cuentan como uno) y las \
		reuniones de su grupo actual; los eventos no cuentan como falta. Alertas: «Faltas seguidas», «Nuevo sin volver» y \
		«Asistencia en caída», con las reglas de la iglesia. Nada se guarda: se calcula al consultar.""")
class SeguimientoAsistenciaController {

	private final SeguimientoAsistenciaService servicio;

	SeguimientoAsistenciaController(SeguimientoAsistenciaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/seguimiento-asistencia")
	@PreAuthorize(Permisos.PUEDE_VER_SEGUIMIENTO_ASISTENCIA)
	@Operation(summary = "Lista de seguimiento", description = """
			Miembros activos con alertas (con `todos=true`, también los demás), de mayor a menor riesgo: alerta más grave, \
			más faltas seguidas y menor porcentaje. Con `asistencia.ver`, toda la iglesia; con `asistencia.grupo`, la red \
			del líder (las personas que lidera directa e indirectamente): su lista de a quién llamar. Paginado.""")
	@ApiResponse(responseCode = "200", description = "Página de personas en seguimiento.")
	@ApiResponse(responseCode = "400", description = "Un filtro no es válido (p. ej. una alerta desconocida).",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<PersonaSeguimientoResponse> lista(@ParameterObject FiltroSeguimiento filtro,
			@ParameterObject @PageableDefault Pageable paginacion) {
		return servicio.lista(filtro, paginacion);
	}

	@GetMapping("/seguimiento-asistencia/conteos")
	@PreAuthorize(Permisos.PUEDE_VER_SEGUIMIENTO_ASISTENCIA)
	@Operation(summary = "Cantidad de personas por alerta", description = """
			Cuántas personas del alcance tienen cada alerta, con los mismos filtros de grupo y nombre que la lista, para \
			los filtros rápidos. Mismos permisos y ámbito que la lista de seguimiento.""")
	@ApiResponse(responseCode = "200", description = "Cantidades por alerta.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConteoAlertasResponse conteos(
			@Parameter(description = "Solo los integrantes de este grupo.", example = "8") @RequestParam(
					required = false) Long grupoId,
			@Parameter(description = "Parte del nombre o apellido.", example = "quispe") @RequestParam(
					required = false) String q) {
		return servicio.conteos(grupoId, q);
	}

	@GetMapping("/seguimiento-asistencia/servicios")
	@PreAuthorize(Permisos.PUEDE_VER_TODA_LA_ASISTENCIA)
	@Operation(summary = "Evolución de los servicios", description = """
			Últimas 12 semanas: asistentes de cada culto programado, personas distintas en los días de culto, asistencias \
			en las reuniones de grupo y porcentaje de asistencia de los grupos. Requiere `asistencia.ver`.""")
	@ApiResponse(responseCode = "200", description = "Series semanales de los servicios.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ServiciosSeguimientoResponse servicios() {
		return servicio.servicios();
	}

	@GetMapping("/grupos/{id:\\d+}/seguimiento")
	@PreAuthorize(Permisos.PUEDE_VER_SEGUIMIENTO_ASISTENCIA)
	@Operation(summary = "Seguimiento de un grupo", description = """
			Evolución semanal del grupo en las últimas 12 semanas (reuniones, asistentes y porcentaje de sus integrantes \
			que asistió) y sus integrantes activos ordenados por riesgo, con su porcentaje, faltas seguidas, última \
			asistencia y alertas. Con `asistencia.ver`, cualquier grupo; con `asistencia.grupo`, solo los que lidera (otro \
			responde 404).""")
	@ApiResponse(responseCode = "200", description = "Seguimiento del grupo.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El grupo no existe en la iglesia del token o el líder no lo lidera.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	SeguimientoGrupoResponse delGrupo(
			@Parameter(description = "Identificador del grupo.", example = "8") @PathVariable Long id) {
		return servicio.delGrupo(id);
	}

}
