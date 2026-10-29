package com.iglesia.api.grupos.controller;

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
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.dto.GuardarGrupoRequest;
import com.iglesia.api.grupos.service.GrupoService;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "Grupos", description = """
		Grupos pequeños de la iglesia del token («célula», «hogar de paz»...; el nombre del tipo lo configura cada \
		iglesia en `/api/configuracion-iglesia`): se reúnen una vez por semana y los lidera un miembro. Consultar \
		exige `grupos.ver` o `miembros.ver`; modificar, `grupos.administrar`. Un grupo no se borra: se desactiva.""")
class GrupoController {

	private static final String DESCRIPCION_ID = "Identificador del grupo.";

	private static final String NO_ENCONTRADO = "El grupo no existe en la iglesia del token.";

	private static final String CONFLICTO = "Ya existe otro grupo con ese nombre (sin distinguir mayúsculas), o el "
			+ "líder o el anfitrión no existe en la iglesia o está desactivado. La propiedad `errores` indica el campo.";

	private static final String DATOS = "Datos inválidos: un campo no cumple las validaciones o el distrito no "
			+ "existe. La propiedad `errores` indica el campo.";

	private final GrupoService servicio;

	GrupoController(GrupoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_GRUPOS)
	@Operation(summary = "Listar los grupos", description = """
			Todos los grupos de la iglesia, ordenados por nombre, incluidos los desactivados, con su líder, \
			anfitrión, ubicación y cantidad de miembros activos. Requiere `grupos.ver` o `miembros.ver`.""")
	@ApiResponse(responseCode = "200", description = "Grupos de la iglesia.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<GrupoResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_GRUPOS)
	@Operation(summary = "Obtener un grupo", description = """
			Datos del grupo. Requiere `grupos.ver` o `miembros.ver`.""")
	@ApiResponse(responseCode = "200", description = "Grupo encontrado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	GrupoResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Crear un grupo", description = """
			Crea un grupo. Requiere `grupos.administrar`. Solo el nombre es obligatorio (único en la iglesia sin \
			distinguir mayúsculas); el líder y el anfitrión deben ser miembros activos de la iglesia.""")
	@ApiResponse(responseCode = "201", description = "Grupo creado.", headers = @Header(name = HttpHeaders.LOCATION, description = "URL del grupo creado."))
	@ApiResponse(responseCode = "400", description = DATOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = CONFLICTO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<GrupoResponse> crear(@Valid @RequestBody GuardarGrupoRequest solicitud) {
		GrupoResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Editar un grupo", description = """
			Reemplaza los datos del grupo, con las mismas reglas que al crear. Un líder o anfitrión que no cambia se \
			conserva aunque se haya desactivado. Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Grupo actualizado.")
	@ApiResponse(responseCode = "400", description = DATOS,
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
	@ApiResponse(responseCode = "409", description = CONFLICTO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	GrupoResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id,
			@Valid @RequestBody GuardarGrupoRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Desactivar o reactivar un grupo", description = """
			Un grupo desactivado ya no se puede asignar a miembros. No se desactiva un grupo con miembros activos \
			(409). Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Grupo con su nuevo estado.")
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
	@ApiResponse(responseCode = "409", description = "El grupo tiene miembros activos.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	GrupoResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

}
