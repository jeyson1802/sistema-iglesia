package com.iglesia.api.asistencia.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.headers.Header;

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

import com.iglesia.api.asistencia.dto.CultoProgramadoResponse;
import com.iglesia.api.asistencia.dto.GuardarCultoRequest;
import com.iglesia.api.asistencia.service.CultoProgramadoService;
import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/cultos-programados")
@Tag(name = "Horarios de culto", description = """
		Cultos fijos de cada iglesia (p. ej. «Culto dominical», domingos 10:00), configurables: una iglesia nueva \
		empieza sin cultos. Consultar exige `asistencia.configurar` o `asistencia.registrar`; modificar, \
		`asistencia.configurar`. No se borran: se desactivan para conservar su historial.""")
class CultoProgramadoController {

	private static final String DESCRIPCION_ID = "Identificador del horario de culto.";

	private static final String NO_ENCONTRADO = "El horario de culto no existe en la iglesia del token.";

	private static final String NOMBRE_REPETIDO = "Ya existe otro culto con ese nombre (sin distinguir mayúsculas).";

	private final CultoProgramadoService servicio;

	CultoProgramadoController(CultoProgramadoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CULTOS)
	@Operation(summary = "Listar horarios de culto", description = """
			Horarios de culto de la iglesia, activos e inactivos, de lunes a domingo y por hora. Requiere \
			`asistencia.configurar` o `asistencia.registrar`.""")
	@ApiResponse(responseCode = "200", description = "Horarios de culto.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<CultoProgramadoResponse> listar() {
		return servicio.listar();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Crear un horario de culto", description = """
			Crea un culto fijo con nombre (único en la iglesia sin distinguir mayúsculas), día de la semana y hora. Aparece en \
			las reuniones de hoy el día que corresponde. Requiere `asistencia.configurar`.""")
	@ApiResponse(responseCode = "201", description = "Horario de culto creado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del horario creado."))
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
	ResponseEntity<CultoProgramadoResponse> crear(@Valid @RequestBody GuardarCultoRequest solicitud) {
		CultoProgramadoResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Editar un horario de culto", description = """
			Cambia el nombre, el día o la hora. Las reuniones ya registradas conservan el nombre y la hora que tenían. Requiere \
			`asistencia.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Horario de culto actualizado.")
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
	CultoProgramadoResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "3") @PathVariable Long id,
			@Valid @RequestBody GuardarCultoRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Desactivar o reactivar un horario de culto", description = """
			Un culto desactivado deja de aparecer en las reuniones de hoy y de contar para el porcentaje de asistencia, pero su \
			historial se conserva. Requiere `asistencia.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Horario de culto con su nuevo estado.")
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
	CultoProgramadoResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "3") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

}
