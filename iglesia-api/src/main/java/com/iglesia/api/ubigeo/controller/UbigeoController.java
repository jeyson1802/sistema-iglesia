package com.iglesia.api.ubigeo.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.ubigeo.dto.UbigeoResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

@RestController
@RequestMapping("/api/ubigeo")
@Tag(name = "Ubigeo", description = """
		Ubigeo oficial del Perú (INEI): departamentos, provincias y distritos para los selectores en cascada de la \
		dirección de miembros y grupos. Es un catálogo común a todas las iglesias y de solo lectura; basta con estar \
		autenticado.""")
class UbigeoController {

	private final UbigeoService servicio;

	UbigeoController(UbigeoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/departamentos")
	@Operation(summary = "Listar departamentos", description = """
			Los 25 departamentos del Perú, ordenados por nombre. Requiere estar autenticado.""")
	@ApiResponse(responseCode = "200", description = "Departamentos.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<UbigeoResponse> departamentos() {
		return servicio.departamentos();
	}

	@GetMapping("/departamentos/{codigo}/provincias")
	@Operation(summary = "Listar provincias de un departamento", description = """
			Provincias del departamento, ordenadas por nombre. Requiere estar autenticado.""")
	@ApiResponse(responseCode = "200", description = "Provincias del departamento.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El departamento no existe.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<UbigeoResponse> provincias(
			@Parameter(description = "Código INEI del departamento (2 dígitos).", example = "15") @PathVariable String codigo) {
		return servicio.provincias(codigo);
	}

	@GetMapping("/provincias/{codigo}/distritos")
	@Operation(summary = "Listar distritos de una provincia", description = """
			Distritos de la provincia, ordenados por nombre. Requiere estar autenticado.""")
	@ApiResponse(responseCode = "200", description = "Distritos de la provincia.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "La provincia no existe.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<UbigeoResponse> distritos(
			@Parameter(description = "Código INEI de la provincia (4 dígitos).", example = "1501") @PathVariable String codigo) {
		return servicio.distritos(codigo);
	}

}
