package com.iglesia.api.aportes.controller;

import java.util.List;

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

import com.iglesia.api.aportes.dto.ConfiguracionAportesResponse;
import com.iglesia.api.aportes.dto.GuardarConfiguracionAportesRequest;
import com.iglesia.api.aportes.dto.MonedaResponse;
import com.iglesia.api.aportes.service.ConfiguracionAportesService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/aportes")
@Tag(name = "Configuración de aportes", description = """
		Moneda en la que la iglesia del token registra sus aportes. Consultarla exige `aportes.ver`, \
		`aportes.registrar` o `aportes.configurar`; cambiarla, `aportes.configurar`, y solo mientras no haya aportes \
		vigentes.""")
class ConfiguracionAportesController {

	private final ConfiguracionAportesService servicio;

	ConfiguracionAportesController(ConfiguracionAportesService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/configuracion")
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONFIGURACION_APORTES)
	@Operation(summary = "Consultar la configuración de aportes", description = """
			Moneda de la iglesia (`PEN` si nunca se configuró) y si todavía se puede cambiar. Requiere \
			`aportes.ver`, `aportes.registrar` o `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Configuración vigente.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConfiguracionAportesResponse obtener() {
		return servicio.obtener();
	}

	@PutMapping("/configuracion")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Cambiar la moneda de los aportes", description = """
			Fija la moneda de la iglesia, una de `GET /api/aportes/monedas`. Solo se puede cambiar mientras no haya \
			aportes vigentes (los anulados no cuentan), para que ningún monto registrado cambie de significado. \
			Requiere `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Configuración guardada.")
	@ApiResponse(responseCode = "400", description = "Datos inválidos o la moneda no está entre las admitidas.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Ya hay aportes vigentes: la moneda no se puede cambiar.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ConfiguracionAportesResponse guardar(@Valid @RequestBody GuardarConfiguracionAportesRequest solicitud) {
		return servicio.guardar(solicitud);
	}

	@GetMapping("/monedas")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Listar las monedas admitidas", description = """
			Monedas que se pueden elegir, con su código ISO 4217, nombre en español y símbolo. Requiere \
			`aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Monedas admitidas.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<MonedaResponse> monedas() {
		return servicio.monedas();
	}

}
