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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.CodigoAsistenciaResponse;
import com.iglesia.api.asistencia.dto.FiltroOcasiones;
import com.iglesia.api.asistencia.dto.OcasionResponse;
import com.iglesia.api.asistencia.service.AsistenciaMiembroService;
import com.iglesia.api.asistencia.service.SeguimientoAsistenciaService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/miembros/{id:\\d+}")
@Tag(name = "Asistencia del miembro", description = """
		La asistencia vista desde la ficha de un miembro: su seguimiento (porcentaje, tendencia, faltas seguidas, \
		alertas, evolución e historial de ocasiones) y el código de su QR personal para el fotocheck. El código es aleatorio y no revela el id, el nombre ni el documento.""")
class AsistenciaMiembroController {

	private static final String DESCRIPCION_ID = "Identificador del miembro.";

	private static final String NO_ENCONTRADO = "El miembro no existe en la iglesia del token o está fuera del ámbito del usuario.";

	private static final String REGLA_OCASIONES = """
			Le corresponden los días de culto (todos los cultos de una misma fecha cuentan como uno: ir a cualquiera es \
			asistir ese día) y las reuniones de su grupo actual, desde su fecha de ingreso y mientras esté activo. Los \
			eventos no cuentan como falta.""";

	private final AsistenciaMiembroService servicio;

	private final SeguimientoAsistenciaService seguimiento;

	AsistenciaMiembroController(AsistenciaMiembroService servicio, SeguimientoAsistenciaService seguimiento) {
		this.servicio = servicio;
		this.seguimiento = seguimiento;
	}

	@GetMapping("/asistencia")
	@PreAuthorize(Permisos.PUEDE_VER_ASISTENCIA_DE_MIEMBRO)
	@Operation(summary = "Seguimiento de asistencia de un miembro", description = REGLA_OCASIONES + """
			 Devuelve el porcentaje de las últimas 8 semanas y el de las 8 anteriores (tendencia), las faltas seguidas, la \
			última asistencia, las alertas según las reglas de la iglesia, la evolución de las últimas 12 semanas y sus \
			últimas 5 reuniones. Con `asistencia.ver`, la de cualquier miembro; con `miembros.ver`, la de los miembros \
			cuya ficha puede ver (el líder, su red; otro responde 404).""")
	@ApiResponse(responseCode = "200", description = "Asistencia del miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AsistenciaMiembroResponse resumen(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id) {
		return seguimiento.delMiembro(id);
	}

	@GetMapping("/asistencia/ocasiones")
	@PreAuthorize(Permisos.PUEDE_VER_ASISTENCIA_DE_MIEMBRO)
	@Operation(summary = "Historial de ocasiones de un miembro", description = REGLA_OCASIONES + """
			 Cada ocasión indica si asistió (`ASISTIO`), faltó (`FALTO`) o fue a un evento (`EXTRA`) y a qué reunión fue. \
			Paginado, lo más reciente primero. Mismos permisos y ámbito que el seguimiento del miembro.""")
	@ApiResponse(responseCode = "200", description = "Página de ocasiones.")
	@ApiResponse(responseCode = "400", description = "La fecha final es anterior a la inicial o un filtro no es válido.",
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
	Page<OcasionResponse> ocasiones(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id,
			@ParameterObject FiltroOcasiones filtro, @ParameterObject @PageableDefault Pageable paginacion) {
		return seguimiento.ocasiones(id, filtro, paginacion);
	}

	@GetMapping("/codigo-asistencia")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Código del QR de asistencia", description = """
			Código del QR personal del miembro y el texto que se codifica en el QR, para armar el fotocheck en el navegador. Si \
			el miembro aún no tiene código (registrado antes de la asistencia), lo genera. Requiere `miembros.ver` y poder ver la \
			ficha del miembro (otro responde 404).""")
	@ApiResponse(responseCode = "200", description = "Código del miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CodigoAsistenciaResponse codigo(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id) {
		return servicio.codigo(id);
	}

	@PostMapping("/codigo-asistencia")
	@PreAuthorize(Permisos.PUEDE_EDITAR_MIEMBROS)
	@Operation(summary = "Regenerar el código del QR", description = """
			Reemplaza el código del QR (p. ej. si se perdió el fotocheck): el anterior deja de valer al instante y hay que \
			volver a descargar o imprimir el fotocheck. Requiere `miembros.editar`.""")
	@ApiResponse(responseCode = "200", description = "Código nuevo del miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CodigoAsistenciaResponse regenerarCodigo(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id) {
		return servicio.regenerarCodigo(id);
	}

}
