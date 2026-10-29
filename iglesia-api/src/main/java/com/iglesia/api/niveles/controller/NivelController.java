package com.iglesia.api.niveles.controller;

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
import com.iglesia.api.niveles.dto.GuardarNivelRequest;
import com.iglesia.api.niveles.dto.NivelResponse;
import com.iglesia.api.niveles.dto.OrdenarNivelesRequest;
import com.iglesia.api.niveles.service.NivelService;

@RestController
@RequestMapping("/api/niveles")
@Tag(name = "Niveles", description = """
		Catálogo de niveles (rangos) de los miembros, configurable por cada iglesia: nombre y orden jerárquico. Solo \
		se ven y modifican los niveles de la iglesia del token. Consultar exige `miembros.ver` o \
		`niveles.administrar`; modificar, `niveles.administrar`. Un nivel no se borra: se desactiva.""")
class NivelController {

	private static final String DESCRIPCION_ID = "Identificador del nivel.";

	private static final String NO_ENCONTRADO = "El nivel no existe en la iglesia del token.";

	private static final String NOMBRE_REPETIDO = "Ya existe otro nivel con ese nombre (sin distinguir mayúsculas).";

	private final NivelService servicio;

	NivelController(NivelService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_NIVELES)
	@Operation(summary = "Listar los niveles", description = """
			Todos los niveles de la iglesia, del más alto al más bajo, incluidos los desactivados \
			(`activo: false`). Requiere `miembros.ver` o `niveles.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Niveles ordenados.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<NivelResponse> listar() {
		return servicio.listar();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_NIVELES)
	@Operation(summary = "Crear un nivel", description = """
			Agrega un nivel al final del orden. Requiere `niveles.administrar`. El nombre es obligatorio (máximo \
			60 caracteres) y único en la iglesia sin distinguir mayúsculas.""")
	@ApiResponse(responseCode = "201", description = "Nivel creado.", headers = @Header(name = HttpHeaders.LOCATION, description = "URL del nivel creado."))
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
	ResponseEntity<NivelResponse> crear(@Valid @RequestBody GuardarNivelRequest solicitud) {
		NivelResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_NIVELES)
	@Operation(summary = "Renombrar un nivel", description = """
			Cambia el nombre del nivel, con las mismas reglas que al crear. Requiere `niveles.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Nivel renombrado.")
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
	NivelResponse renombrar(@Parameter(description = DESCRIPCION_ID, example = "3") @PathVariable Long id,
			@Valid @RequestBody GuardarNivelRequest solicitud) {
		return servicio.renombrar(id, solicitud);
	}

	@PutMapping("/orden")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_NIVELES)
	@Operation(summary = "Reordenar los niveles", description = """
			Fija el orden jerárquico de los niveles: el primero de la lista es el más alto. Deben venir todos los \
			niveles de la iglesia (activos e inactivos), una sola vez cada uno. Requiere `niveles.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Niveles en el nuevo orden.")
	@ApiResponse(responseCode = "400", description = "Datos inválidos o la lista no trae todos los niveles de la iglesia exactamente una vez.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<NivelResponse> ordenar(@Valid @RequestBody OrdenarNivelesRequest solicitud) {
		return servicio.ordenar(solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_NIVELES)
	@Operation(summary = "Desactivar o reactivar un nivel", description = """
			Un nivel desactivado ya no se puede asignar, pero los miembros que lo tienen lo conservan. Requiere \
			`niveles.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Nivel con su nuevo estado.")
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
	NivelResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "3") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

}
