package com.iglesia.api.grupos.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.dto.CandidatoGrupoResponse;
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.dto.GuardarGrupoRequest;
import com.iglesia.api.grupos.dto.IntegranteGrupoResponse;
import com.iglesia.api.grupos.service.GrupoService;
import com.iglesia.api.miembros.constant.LimitesMiembro;

@RestController
@RequestMapping("/api/grupos")
@Tag(name = "Grupos", description = """
		Grupos pequeños de la iglesia del token («célula», «hogar de paz»...; el nombre del tipo lo configura cada \
		iglesia en `/api/configuracion-iglesia`): se reúnen una vez por semana y los lidera un miembro. Un grupo no \
		tiene nombre: es «el grupo de» su líder, que es obligatorio y tiene un solo grupo. Consultar exige \
		`grupos.ver` o `miembros.ver`; modificar, `grupos.administrar`. Un grupo no se borra: se desactiva.""")
class GrupoController {

	private static final String DESCRIPCION_ID = "Identificador del grupo.";

	private static final String NO_ENCONTRADO = "El grupo no existe en la iglesia del token.";

	private static final String CONFLICTO = "El líder ya tiene otro grupo, o el líder o el anfitrión no existe en la "
			+ "iglesia o está desactivado. La propiedad `errores` indica el campo.";

	private static final String DATOS = "Datos inválidos: falta el líder, un campo no cumple las validaciones o el "
			+ "distrito no existe. La propiedad `errores` indica el campo.";

	private static final String DESCRIPCION_MIEMBRO = "Identificador del miembro.";

	private final GrupoService servicio;

	GrupoController(GrupoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_GRUPOS)
	@Operation(summary = "Listar los grupos", description = """
			Todos los grupos de la iglesia, ordenados por el nombre de su líder, incluidos los desactivados, con su líder, \
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
			Crea un grupo. Requiere `grupos.administrar`. Solo el líder es obligatorio: debe ser un miembro activo \
			de la iglesia sin otro grupo. El anfitrión, si se indica, también debe estar activo.""")
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

	@GetMapping("/{id}/miembros")
	@PreAuthorize(Permisos.PUEDE_VER_GRUPOS)
	@Operation(summary = "Listar los miembros del grupo", description = """
			Miembros activos que pertenecen al grupo, ordenados por apellidos, con su nivel. Requiere `grupos.ver` \
			o `miembros.ver`.""")
	@ApiResponse(responseCode = "200", description = "Integrantes del grupo (vacía si no tiene).")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<IntegranteGrupoResponse> integrantes(
			@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id) {
		return servicio.integrantes(id);
	}

	@GetMapping("/{id}/candidatos")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Buscar miembros para agregar al grupo", description = """
			Miembros activos que no están en el grupo, con el grupo en el que están ahora (agregarlos los mueve). Con \
			texto, hasta 20 cuyo nombre lo contiene, ordenados por apellidos; sin texto, los 5 últimos en llegar a la \
			iglesia (por fecha de ingreso o, si no tienen, por la de registro). Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Candidatos.")
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
	List<CandidatoGrupoResponse> candidatos(
			@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id,
			@Parameter(description = "Parte de los nombres y apellidos, sin distinguir mayúsculas ni tildes. Vacío: "
					+ "los 5 últimos en llegar a la iglesia.", example = "rosa") @RequestParam(required = false) @Size(
							max = LimitesMiembro.BUSQUEDA_MAX) String q) {
		return servicio.candidatos(id, q);
	}

	@PutMapping("/{id}/miembros/{miembroId}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Agregar un miembro al grupo", description = """
			Pone al miembro en el grupo: lo agrega o, si estaba en otro, lo mueve (pertenece a un solo grupo). Si ya \
			estaba en este grupo no cambia nada. Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Miembro agregado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El grupo o el miembro no existe en la iglesia del token.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El grupo o el miembro está desactivado.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	IntegranteGrupoResponse agregarIntegrante(
			@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id,
			@Parameter(description = DESCRIPCION_MIEMBRO, example = "31") @PathVariable Long miembroId) {
		return servicio.agregarIntegrante(id, miembroId);
	}

	@DeleteMapping("/{id}/miembros/{miembroId}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Quitar un miembro del grupo", description = """
			Saca al miembro del grupo; queda sin grupo. No borra al miembro. Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Miembro quitado del grupo.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El grupo o el miembro no existe, o el miembro no está en ese grupo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> quitarIntegrante(
			@Parameter(description = DESCRIPCION_ID, example = "4") @PathVariable Long id,
			@Parameter(description = DESCRIPCION_MIEMBRO, example = "31") @PathVariable Long miembroId) {
		servicio.quitarIntegrante(id, miembroId);
		return ResponseEntity.noContent().build();
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
