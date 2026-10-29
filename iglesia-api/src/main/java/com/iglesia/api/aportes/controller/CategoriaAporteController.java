package com.iglesia.api.aportes.controller;

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

import com.iglesia.api.aportes.dto.CategoriaAporteResponse;
import com.iglesia.api.aportes.dto.GuardarCategoriaAporteRequest;
import com.iglesia.api.aportes.dto.OrdenarCategoriasAporteRequest;
import com.iglesia.api.aportes.service.CategoriaAporteService;
import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/categorias-aporte")
@Tag(name = "Categorías de aporte", description = """
		Conceptos de los aportes (p. ej. «Diezmo», «Ofrenda», «Misiones»), configurables por cada iglesia. Toda \
		iglesia nace con Diezmo (exige aportante) y Ofrenda (admite anónimos). Consultar exige `aportes.ver`, \
		`aportes.registrar` o `aportes.configurar`; modificar, `aportes.configurar`. No se borran: se desactivan.""")
class CategoriaAporteController {

	private static final String DESCRIPCION_ID = "Identificador de la categoría.";

	private static final String NO_ENCONTRADA = "La categoría no existe en la iglesia del token.";

	private static final String NOMBRE_REPETIDO = "Ya existe otra categoría con ese nombre (sin distinguir mayúsculas).";

	private final CategoriaAporteService servicio;

	CategoriaAporteController(CategoriaAporteService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONFIGURACION_APORTES)
	@Operation(summary = "Listar las categorías de aporte", description = """
			Todas las categorías de la iglesia en su orden, incluidas las desactivadas (`activo: false`). Requiere \
			`aportes.ver`, `aportes.registrar` o `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Categorías ordenadas.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<CategoriaAporteResponse> listar() {
		return servicio.listar();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Crear una categoría de aporte", description = """
			Agrega una categoría al final del orden. El nombre es obligatorio (máximo 60 caracteres) y único en la \
			iglesia sin distinguir mayúsculas. Requiere `aportes.configurar`.""")
	@ApiResponse(responseCode = "201", description = "Categoría creada.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la categoría creada."))
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
	ResponseEntity<CategoriaAporteResponse> crear(@Valid @RequestBody GuardarCategoriaAporteRequest solicitud) {
		CategoriaAporteResponse creada = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creada.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Editar una categoría de aporte", description = """
			Cambia el nombre y si exige aportante, con las mismas reglas que al crear. Los aportes ya registrados no \
			cambian. Requiere `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Categoría actualizada.")
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
	CategoriaAporteResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Valid @RequestBody GuardarCategoriaAporteRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PutMapping("/orden")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Reordenar las categorías de aporte", description = """
			Fija el orden en que se ofrecen las categorías. Deben venir todas las de la iglesia (activas e \
			inactivas), una sola vez cada una. Requiere `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Categorías en el nuevo orden.")
	@ApiResponse(responseCode = "400", description = "Datos inválidos o la lista no trae todas las categorías de la iglesia exactamente una vez.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<CategoriaAporteResponse> ordenar(@Valid @RequestBody OrdenarCategoriasAporteRequest solicitud) {
		return servicio.ordenar(solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_APORTES)
	@Operation(summary = "Desactivar o reactivar una categoría de aporte", description = """
			Una categoría desactivada ya no se ofrece para aportes nuevos, pero sus aportes la conservan y siguen \
			contando en los totales. Requiere `aportes.configurar`.""")
	@ApiResponse(responseCode = "200", description = "Categoría con su nuevo estado.")
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
	CategoriaAporteResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

}
