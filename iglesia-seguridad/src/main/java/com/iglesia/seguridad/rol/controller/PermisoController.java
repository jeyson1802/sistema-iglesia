package com.iglesia.seguridad.rol.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.rol.dto.PermisoResponse;
import com.iglesia.seguridad.rol.service.RolService;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = """
		Catálogo fijo de permisos (`<modulo>.<accion>`) con los que se arman los roles. Cada permiso corresponde a \
		código real, por eso solo se agregan por migración y no se pueden crear desde la API.""")
class PermisoController {

	private final RolService servicio;

	PermisoController(RolService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_ROLES)
	@Operation(summary = "Listar el catálogo de permisos", description = """
			Todos los permisos del sistema, ordenados por módulo y por su orden dentro del módulo. Requiere el \
			permiso `roles.ver`. Sirve para armar el formulario de roles agrupando por `modulo`.""")
	@ApiResponse(responseCode = "200", description = "Catálogo de permisos.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<PermisoResponse> listar() {
		return servicio.listarPermisos();
	}

}
