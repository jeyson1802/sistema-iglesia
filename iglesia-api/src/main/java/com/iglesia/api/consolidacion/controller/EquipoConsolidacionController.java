package com.iglesia.api.consolidacion.controller;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.dto.AgregarConsolidadorRequest;
import com.iglesia.api.consolidacion.dto.ConsolidadorResponse;
import com.iglesia.api.consolidacion.service.EquipoConsolidacionService;

@RestController
@RequestMapping("/api/consolidacion/equipo")
@Tag(name = "Consolidación: equipo", description = """
		Miembros que consolidan (llaman o visitan a las personas). El equipo es la lista de candidatos al repartir \
		una ronda; no da permisos: para registrar contactos, cada uno necesita un usuario con \
		`consolidacion.contactar`. Requiere `consolidacion.administrar`.""")
class EquipoConsolidacionController {

	private static final String DESCRIPCION_MIEMBRO = "Miembro del equipo.";

	private static final String NO_ENCONTRADO = "El miembro no existe en la iglesia del token o no forma parte del equipo.";

	private static final String CONFLICTO = "Ya forma parte del equipo o la persona está inactiva.";

	private final EquipoConsolidacionService servicio;

	EquipoConsolidacionController(EquipoConsolidacionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Listar el equipo de consolidación", description = """
			Consolidadores activos primero y luego los inactivos, por nombre, con sus casos abiertos y los contactos que \
			hicieron en los últimos 30 días (para repartir con equilibrio). Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Equipo de consolidación.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<ConsolidadorResponse> listar() {
		return servicio.listar();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Agregar un consolidador", description = """
			Agrega un miembro activo al equipo; si estuvo antes, lo reactiva. 409 si ya está activo en el equipo o si la persona \
			está inactiva. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "201", description = "Consolidador agregado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El miembro no existe en la iglesia del token.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = CONFLICTO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.CREATED)
	ConsolidadorResponse agregar(@Valid @RequestBody AgregarConsolidadorRequest solicitud) {
		return servicio.agregar(solicitud.miembroId());
	}

	@PatchMapping("/{miembroId}/estado")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Activar o quitar del equipo", description = """
			Un consolidador inactivo deja de entrar en los repartos. Sus casos abiertos no cambian: la respuesta indica cuántos \
			tiene, para reasignarlos. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Consolidador con su nuevo estado.")
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
	ConsolidadorResponse cambiarEstado(@Parameter(description = DESCRIPCION_MIEMBRO, example = "25") @PathVariable Long miembroId,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(miembroId, solicitud.activo());
	}

}
