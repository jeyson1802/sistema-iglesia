package com.iglesia.api.aportes.controller;

import java.net.URI;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.iglesia.api.aportes.dto.AnularAporteRequest;
import com.iglesia.api.aportes.dto.AporteResponse;
import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.dto.GuardarAporteRequest;
import com.iglesia.api.aportes.dto.ResumenAportesResponse;
import com.iglesia.api.aportes.service.AporteService;
import com.iglesia.api.aportes.service.ResumenAportesService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/aportes")
@Tag(name = "Aportes", description = """
		Diezmos, ofrendas y demás aportes de los miembros de la iglesia del token (o anónimos, si la categoría lo \
		admite). Leer exige `aportes.ver` y respeta el ámbito: el administrador de iglesia ve todos; cualquier otro \
		usuario vinculado a un miembro (p. ej. un líder), solo los suyos; sin miembro vinculado, ninguno. Registrar \
		exige `aportes.registrar`; corregir, `aportes.editar`; anular, `aportes.anular`. Un aporte nunca se borra: \
		se anula con un motivo y deja de contar en los totales.""")
class AporteController {

	private static final String DESCRIPCION_ID = "Identificador del aporte.";

	private static final String NO_ENCONTRADO = "El aporte no existe en la iglesia del token o está fuera del ámbito del usuario.";

	private static final String RANGO_INVALIDO = "Datos inválidos: faltan `desde` o `hasta`, `hasta` es anterior a `desde` o el rango supera 366 días.";

	private final AporteService servicio;

	private final ResumenAportesService resumen;

	AporteController(AporteService servicio, ResumenAportesService resumen) {
		this.servicio = servicio;
		this.resumen = resumen;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_APORTES)
	@Operation(summary = "Listar aportes", description = """
			Página de aportes del rango de fechas (obligatorio, hasta 366 días), del más reciente al más antiguo, \
			con filtros opcionales por categoría, medio y miembro. Los anulados solo aparecen con \
			`incluirAnulados=true`. Un líder ve solo sus propios aportes (el filtro `miembroId` se ignora). Orden \
			admitido: `fecha`, `monto`, `registradoEn`. Requiere `aportes.ver`.""")
	@ApiResponse(responseCode = "200", description = "Página de aportes.")
	@ApiResponse(responseCode = "400", description = RANGO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<AporteResponse> listar(@Valid @ParameterObject FiltroAportes filtro,
			@ParameterObject @PageableDefault(sort = { "fecha", "registradoEn" }, direction = Sort.Direction.DESC) Pageable paginacion) {
		return servicio.listar(filtro, paginacion);
	}

	@GetMapping("/resumen")
	@PreAuthorize(Permisos.PUEDE_VER_APORTES)
	@Operation(summary = "Totales de los aportes", description = """
			Total, cantidad y desglose por categoría y por medio de los aportes vigentes (nunca cuenta los \
			anulados) del rango y filtros indicados, en la moneda de la iglesia, más el total del período anterior \
			de la misma duración para comparar. Respeta el mismo ámbito que el listado. Requiere `aportes.ver`.""")
	@ApiResponse(responseCode = "200", description = "Totales del rango.")
	@ApiResponse(responseCode = "400", description = RANGO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResumenAportesResponse resumen(@Valid @ParameterObject FiltroAportes filtro) {
		return resumen.resumen(filtro);
	}

	@GetMapping("/{id:\\d+}")
	@PreAuthorize(Permisos.PUEDE_VER_APORTES)
	@Operation(summary = "Consultar un aporte", description = """
			Detalle de un aporte, incluidos los anulados con su motivo. Un aporte fuera del ámbito del usuario \
			responde 404. Requiere `aportes.ver`.""")
	@ApiResponse(responseCode = "200", description = "Aporte encontrado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AporteResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "45") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_REGISTRAR_APORTES)
	@Operation(summary = "Registrar un aporte", description = """
			Registra un aporte en la moneda de la iglesia. La fecha no puede ser futura y el monto debe ser mayor \
			que 0 con hasta 2 decimales. Si la categoría exige aportante, `miembroId` es obligatorio; si no, puede \
			omitirse (anónimo). La categoría y el miembro deben estar activos. Con `EFECTIVO` se ignora el número \
			de operación. Guarda quién lo registró (del token). Requiere `aportes.registrar`.""")
	@ApiResponse(responseCode = "201", description = "Aporte registrado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del aporte registrado."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "La categoría o el miembro no existen o están desactivados, o la categoría exige aportante y no se indicó.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<AporteResponse> crear(@Valid @RequestBody GuardarAporteRequest solicitud) {
		AporteResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id:\\d+}")
	@PreAuthorize(Permisos.PUEDE_EDITAR_APORTES)
	@Operation(summary = "Corregir un aporte", description = """
			Cambia todos los datos del aporte con las mismas reglas del registro; la categoría y el aportante que ya \
			tenía se conservan aunque se hayan desactivado. Guarda quién y cuándo lo modificó. Un aporte anulado no \
			se corrige. Requiere `aportes.editar`.""")
	@ApiResponse(responseCode = "200", description = "Aporte corregido.")
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
	@ApiResponse(responseCode = "409", description = "El aporte está anulado, o la categoría o el miembro nuevos no se pueden asignar.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AporteResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "45") @PathVariable Long id,
			@Valid @RequestBody GuardarAporteRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PatchMapping("/{id:\\d+}/anulacion")
	@PreAuthorize(Permisos.PUEDE_ANULAR_APORTES)
	@Operation(summary = "Anular un aporte", description = """
			Anula el aporte con un motivo obligatorio: deja de contar en los totales pero se conserva en el \
			historial. No se puede deshacer; si fue un error, se registra de nuevo. Requiere `aportes.anular`.""")
	@ApiResponse(responseCode = "200", description = "Aporte anulado.")
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
	@ApiResponse(responseCode = "409", description = "El aporte ya estaba anulado.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AporteResponse anular(@Parameter(description = DESCRIPCION_ID, example = "45") @PathVariable Long id,
			@Valid @RequestBody AnularAporteRequest solicitud) {
		return servicio.anular(id, solicitud);
	}

}
