package com.iglesia.api.consolidacion.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.dto.GuardarResultadoRequest;
import com.iglesia.api.consolidacion.dto.OrdenResultadosRequest;
import com.iglesia.api.consolidacion.dto.ResultadoConsolidacionResponse;
import com.iglesia.api.consolidacion.service.ResultadoConsolidacionService;

@RestController
@RequestMapping("/api/consolidacion/resultados")
@Tag(name = "Consolidación: resultados", description = """
		Catálogo de resultados de una iteración de consolidación (p. ej. «Volverá a asistir»), configurable por \
		iglesia: se renombran, se ordenan y se desactivan, nunca se borran, para conservar el historial. Consultar \
		admite cualquier permiso de consolidación; modificar exige `consolidacion.administrar`.""")
class ResultadoConsolidacionController {

	private static final String DESCRIPCION_ID = "Identificador del resultado.";

	private static final String NO_ENCONTRADO = "El resultado no existe en la iglesia del token.";

	private static final String NOMBRE_REPETIDO = "Ya existe otro resultado con ese nombre (sin distinguir mayúsculas).";

	private final ResultadoConsolidacionService servicio;

	ResultadoConsolidacionController(ResultadoConsolidacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONSOLIDACION)
	@Operation(summary = "Listar resultados", description = """
			Resultados de la iglesia, activos e inactivos, en su orden. Requiere `consolidacion.administrar`, \
			`consolidacion.ver` o `consolidacion.contactar` (el consolidador los elige al cerrar un caso).""")
	@ApiResponse(responseCode = "200", description = "Resultados en su orden.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<ResultadoConsolidacionResponse> listar() {
		return servicio.listar();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Crear un resultado", description = """
			Agrega un resultado al final del catálogo. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "201", description = "Resultado creado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del resultado creado."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = NOMBRE_REPETIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<ResultadoConsolidacionResponse> crear(@Valid @RequestBody GuardarResultadoRequest solicitud) {
		ResultadoConsolidacionResponse creado = servicio.crear(solicitud.nombre());
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/orden")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Reordenar los resultados", description = """
			Recibe todos los resultados de la iglesia en el orden deseado (400 si falta o sobra alguno). Requiere \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Resultados en su nuevo orden.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<ResultadoConsolidacionResponse> ordenar(@Valid @RequestBody OrdenResultadosRequest solicitud) {
		return servicio.ordenar(solicitud.ids());
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Renombrar un resultado", description = """
			Cambia el nombre; los casos ya cerrados muestran el nombre nuevo. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Resultado actualizado.")
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
	@ApiResponse(responseCode = "409", description = NOMBRE_REPETIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResultadoConsolidacionResponse renombrar(@Parameter(description = DESCRIPCION_ID, example = "2") @PathVariable Long id,
			@Valid @RequestBody GuardarResultadoRequest solicitud) {
		return servicio.renombrar(id, solicitud.nombre());
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Desactivar o reactivar un resultado", description = """
			Un resultado inactivo ya no se ofrece al cerrar casos, pero los casos cerrados con él lo conservan. Requiere \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Resultado con su nuevo estado.")
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
	ResultadoConsolidacionResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "2") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

}
