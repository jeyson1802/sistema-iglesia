package com.iglesia.api.grupos.controller;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.dto.ConfiguracionIglesiaResponse;
import com.iglesia.api.grupos.dto.GuardarConfiguracionIglesiaRequest;
import com.iglesia.api.grupos.service.ConfiguracionIglesiaService;

@RestController
@RequestMapping("/api/configuracion-iglesia")
@Tag(name = "Configuración de la iglesia", description = """
		Preferencias de la iglesia del token. Por ahora, cómo llama a sus grupos (p. ej. «Célula» / «Células»), que \
		el frontend usa en el menú, los títulos y las etiquetas.""")
class ConfiguracionIglesiaController {

	private final ConfiguracionIglesiaService servicio;

	ConfiguracionIglesiaController(ConfiguracionIglesiaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Obtener la configuración", description = """
			Nombre de los grupos en singular y plural; «Grupo» / «Grupos» si la iglesia no lo configuró o si el \
			usuario no opera dentro de una iglesia. Requiere estar autenticado.""")
	@ApiResponse(responseCode = "200", description = "Configuración de la iglesia.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario tiene una contraseña temporal que debe cambiar antes de continuar.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConfiguracionIglesiaResponse obtener() {
		return servicio.obtener();
	}

	@PutMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_GRUPOS)
	@Operation(summary = "Configurar el nombre de los grupos", description = """
			Guarda cómo llama la iglesia a sus grupos, en singular y plural (máximo 40 caracteres cada uno). \
			Requiere `grupos.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Configuración guardada.")
	@ApiResponse(responseCode = "400", description = "Datos inválidos, o el usuario no opera dentro de una iglesia.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConfiguracionIglesiaResponse guardar(@Valid @RequestBody GuardarConfiguracionIglesiaRequest solicitud) {
		return servicio.guardar(solicitud);
	}

}
