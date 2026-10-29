package com.iglesia.api.iglesia.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.service.IglesiaService;

@RestController
@RequestMapping("/api/iglesias")
@Tag(name = "Iglesias", description = """
		Catálogo de iglesias (tenants) del sistema. Solo lo usa el administrador del sistema: consultar exige el \
		permiso `iglesias.ver` y dar de alta o renombrar, `iglesias.administrar`. Dar de alta una iglesia es \
		insertar una fila; su primer administrador se crea después en iglesia-seguridad (`POST /api/usuarios` con \
		este `iglesiaId`). El catálogo no se filtra por la iglesia del token: siempre se ve completo.""")
class IglesiaController {

	private static final String DESCRIPCION_ID = "Identificador de la iglesia.";

	private static final String NO_ENCONTRADA = "La iglesia no existe.";

	private static final String NOMBRE_REPETIDO = "Ya existe otra iglesia con ese nombre (sin distinguir mayúsculas).";

	private final IglesiaService servicio;

	IglesiaController(IglesiaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_IGLESIAS)
	@Operation(summary = "Listar las iglesias", description = """
			Todas las iglesias del catálogo, ordenadas por nombre. Requiere el permiso `iglesias.ver`.""")
	@ApiResponse(responseCode = "200", description = "Iglesias del catálogo.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<IglesiaResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_IGLESIAS)
	@Operation(summary = "Obtener una iglesia", description = """
			Datos de una iglesia del catálogo. Requiere el permiso `iglesias.ver`.""")
	@ApiResponse(responseCode = "200", description = "Iglesia encontrada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	IglesiaResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Dar de alta una iglesia", description = """
			Agrega una iglesia al catálogo. Requiere el permiso `iglesias.administrar`.

			Reglas: el nombre es obligatorio (máximo 150 caracteres), se guarda sin espacios al inicio ni al final \
			y es único sin distinguir mayúsculas. La iglesia nace sin usuarios: su primer administrador se crea en \
			iglesia-seguridad con `POST /api/usuarios`, indicando el `id` devuelto como `iglesiaId`.""")
	@ApiResponse(responseCode = "201", description = "Iglesia creada.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la iglesia creada."))
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
	ResponseEntity<IglesiaResponse> crear(@Valid @RequestBody GuardarIglesiaRequest solicitud) {
		IglesiaResponse creada = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creada.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Renombrar una iglesia", description = """
			Cambia el nombre de una iglesia; es el único dato editable. Requiere el permiso \
			`iglesias.administrar`. Mismas reglas que al crear; se puede cambiar solo las mayúsculas del nombre \
			actual.""")
	@ApiResponse(responseCode = "200", description = "Iglesia renombrada.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = NOMBRE_REPETIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	IglesiaResponse renombrar(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Valid @RequestBody GuardarIglesiaRequest solicitud) {
		return servicio.renombrar(id, solicitud);
	}

}
