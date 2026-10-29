package com.iglesia.api.asistencia.controller;

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

import com.iglesia.api.asistencia.dto.ReglasSeguimientoDto;
import com.iglesia.api.asistencia.service.ConfiguracionSeguimientoService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/configuracion-seguimiento")
@Tag(name = "Reglas de seguimiento", description = """
		Valores con los que la iglesia decide sus alertas de seguimiento de asistencia: cuántas faltas seguidas, cuántos \
		puntos de caída y cuántos días alguien sigue siendo nuevo. Sin configurar valen 3, 30 y 90.""")
class ConfiguracionSeguimientoController {

	private final ConfiguracionSeguimientoService servicio;

	ConfiguracionSeguimientoController(ConfiguracionSeguimientoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_REGLAS_SEGUIMIENTO)
	@Operation(summary = "Reglas de seguimiento", description = """
			Reglas de las alertas de la iglesia del token (o las de por defecto si nunca se configuraron). Requiere \
			`asistencia.ver` o `asistencia.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Reglas vigentes.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ReglasSeguimientoDto obtener() {
		return ReglasSeguimientoDto.de(servicio.reglas());
	}

	@PutMapping
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Cambiar las reglas de seguimiento", description = """
			Reemplaza las reglas de las alertas. Las alertas se recalculan al instante en la siguiente consulta. Requiere \
			`asistencia.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Reglas guardadas.")
	@ApiResponse(responseCode = "400", description = "Algún valor está fuera de sus límites (faltas 2–10, puntos 10–80, días 30–180).",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ReglasSeguimientoDto guardar(@Valid @RequestBody ReglasSeguimientoDto reglas) {
		return ReglasSeguimientoDto.de(servicio.guardar(reglas.aModelo()));
	}

}
