package com.iglesia.api.asistencia.controller;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.asistencia.dto.AbrirReunionRequest;
import com.iglesia.api.asistencia.dto.CrearEventoRequest;
import com.iglesia.api.asistencia.dto.FiltroReuniones;
import com.iglesia.api.asistencia.dto.ReunionHoyResponse;
import com.iglesia.api.asistencia.dto.ReunionResponse;
import com.iglesia.api.asistencia.model.ReunionAbierta;
import com.iglesia.api.asistencia.service.ReunionService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/reuniones")
@Tag(name = "Reuniones", description = """
		Cada ocurrencia concreta donde se toma asistencia: el culto de un día, la reunión semanal de un grupo o cada \
		día de un evento. Las de cultos y grupos se crean al abrirlas para tomar asistencia; los eventos, al crearlos. \
		Quien tiene `asistencia.registrar` actúa en todas; el líder con `asistencia.grupo`, solo en las de los grupos \
		que lidera (otra responde 404). El historial completo exige `asistencia.ver`.""")
class ReunionController {

	private static final String DESCRIPCION_ID = "Identificador de la reunión.";

	private static final String NO_ENCONTRADA = "La reunión no existe en la iglesia del token o está fuera del ámbito del usuario.";

	private static final String ABRIR_INVALIDO = "Datos inválidos: falta la fecha o es futura, o no se indicó exactamente uno entre `cultoProgramadoId` y `grupoId`.";

	private static final String ORIGEN_NO_ENCONTRADO = "El culto o el grupo no existe en la iglesia del token, o está fuera del ámbito del usuario.";

	private static final String EVENTO_INVALIDO = "Datos inválidos: nombre vacío o de largo no admitido, o `fechaFin` anterior a `fechaInicio` o a más de 6 días de ella.";

	private static final String NO_ELIMINABLE = "La reunión no es un evento o el evento ya tiene asistentes.";

	private static final String RANGO_INVALIDO = "Datos inválidos: `hasta` es anterior a `desde`.";

	private final ReunionService servicio;

	ReunionController(ReunionService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/hoy")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Reuniones de hoy", description = """
			Reuniones de hoy donde el usuario puede tomar asistencia, por hora: las ya abiertas (con `id` y su total) y, por \
			abrir (sin `id`), los cultos activos del día y los grupos activos que se reúnen hoy. Con `asistencia.registrar` ve \
			todas; con `asistencia.grupo`, solo las de los grupos que lidera. Requiere `asistencia.registrar` o \
			`asistencia.grupo`.""")
	@ApiResponse(responseCode = "200", description = "Reuniones de hoy (puede estar vacía).")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<ReunionHoyResponse> hoy() {
		return servicio.hoy();
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Abrir la reunión de un culto o un grupo", description = """
			Devuelve la reunión del culto o del grupo en esa fecha y, si aún no existe, la crea: no hay reuniones vacías de días \
			en que no se tomó asistencia. Dos equipos que la abren a la vez obtienen la misma. La fecha no puede ser futura y \
			puede no coincidir con el día programado. Solo quien tiene `asistencia.registrar` abre cultos; el líder, solo las de \
			sus grupos (otra responde 404). Requiere `asistencia.registrar` o `asistencia.grupo`.""")
	@ApiResponse(responseCode = "201", description = "Reunión creada ahora.")
	@ApiResponse(responseCode = "200", description = "La reunión ya existía.")
	@ApiResponse(responseCode = "400", description = ABRIR_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = ORIGEN_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<ReunionResponse> abrir(@Valid @RequestBody AbrirReunionRequest solicitud) {
		ReunionAbierta abierta = servicio.abrir(solicitud);
		return ResponseEntity.status(abierta.creada() ? HttpStatus.CREATED : HttpStatus.OK).body(abierta.reunion());
	}

	@PostMapping("/eventos")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Crear un evento", description = """
			Crea un evento (convención, vigilia, bautismo…) con una reunión por día, de `fechaInicio` a `fechaFin` (hasta 7 \
			días; sin `fechaFin`, un solo día). Puede ser futuro, pero la asistencia solo se toma desde su fecha. Requiere \
			`asistencia.configurar`.""")
	@ApiResponse(responseCode = "201", description = "Reuniones creadas, una por día.")
	@ApiResponse(responseCode = "400", description = EVENTO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<List<ReunionResponse>> crearEvento(@Valid @RequestBody CrearEventoRequest solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearEvento(solicitud));
	}

	@DeleteMapping("/{id:\\d+}")
	@PreAuthorize(Permisos.PUEDE_CONFIGURAR_ASISTENCIA)
	@Operation(summary = "Eliminar un evento", description = """
			Elimina la reunión de un evento creado por error. Solo eventos y sin asistentes: las reuniones de cultos y grupos, o \
			un evento con asistentes, responden 409. Requiere `asistencia.configurar`.""")
	@ApiResponse(responseCode = "204", description = "Evento eliminado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = NO_ELIMINABLE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> eliminarEvento(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id) {
		servicio.eliminarEvento(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_HISTORIAL_ASISTENCIA)
	@Operation(summary = "Historial de reuniones", description = """
			Página de reuniones con su total de asistentes, de la más reciente a la más antigua, con filtros opcionales de rango, \
			tipo y grupo. Con `asistencia.ver` ve todas; con `asistencia.grupo`, solo las de los grupos que lidera. Orden \
			admitido: `fecha`, `hora`, `nombre`. Requiere `asistencia.ver` o `asistencia.grupo`.""")
	@ApiResponse(responseCode = "200", description = "Página de reuniones.")
	@ApiResponse(responseCode = "400", description = RANGO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<ReunionResponse> historial(@Valid @ParameterObject FiltroReuniones filtro,
			@ParameterObject @PageableDefault(sort = { "fecha", "hora" }, direction = Sort.Direction.DESC) Pageable paginacion) {
		return servicio.historial(filtro, paginacion);
	}

	@GetMapping("/{id:\\d+}")
	@PreAuthorize(Permisos.PUEDE_VER_ASISTENTES)
	@Operation(summary = "Consultar una reunión", description = """
			Datos de la reunión y su total de asistentes. La ve quien puede tomar asistencia en ella o consultar el historial; \
			fuera de su ámbito responde 404. Requiere `asistencia.ver`, `asistencia.registrar` o `asistencia.grupo`.""")
	@ApiResponse(responseCode = "200", description = "Reunión encontrada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ReunionResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id) {
		return servicio.obtener(id);
	}

}
