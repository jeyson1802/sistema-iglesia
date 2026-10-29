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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.asistencia.dto.AsistenteResponse;
import com.iglesia.api.asistencia.dto.MarcaResponse;
import com.iglesia.api.asistencia.dto.MarcarCodigoRequest;
import com.iglesia.api.asistencia.dto.MarcarMiembroRequest;
import com.iglesia.api.asistencia.dto.MiembroReunionResponse;
import com.iglesia.api.asistencia.model.Marca;
import com.iglesia.api.asistencia.service.AsistenciaService;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/reuniones/{id:\\d+}")
@Tag(name = "Toma de asistencia", description = """
		Marcar y quitar asistencias en una reunión (con el código del QR o a mano) y las listas de la pantalla de \
		escaneo. No se toma asistencia en una reunión de fecha futura (400). Quien tiene `asistencia.registrar` marca \
		en todas las reuniones; el líder con `asistencia.grupo`, solo en las de sus grupos (otra responde 404).""")
class AsistenciaController {

	private static final String DESCRIPCION_ID = "Identificador de la reunión.";

	private static final String NO_ENCONTRADA = "La reunión no existe en la iglesia del token o está fuera del ámbito del usuario.";

	private static final String CODIGO_INVALIDO = "El texto leído no es un código de asistencia («Este QR no es un código de asistencia»), o la reunión es de una fecha futura.";

	private static final String MARCA_INVALIDA = "Datos inválidos: falta `miembroId`, o la reunión es de una fecha futura.";

	private static final String CODIGO_NO_ENCONTRADO = "La reunión no existe o está fuera del ámbito, o el código no pertenece a ningún miembro de esta iglesia («Código no encontrado en esta iglesia»).";

	private static final String MIEMBRO_NO_ENCONTRADO = "La reunión o el miembro no existen en la iglesia del token, o la reunión está fuera del ámbito.";

	private static final String MIEMBRO_INACTIVO = "El miembro está desactivado.";

	private static final String NO_MARCADO = "La reunión no existe o está fuera del ámbito, o el miembro no está marcado en ella.";

	private static final String SIN_INTEGRANTES = "La reunión no es de un grupo.";

	private final AsistenciaService servicio;

	AsistenciaController(AsistenciaService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/asistencias/codigo")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Marcar con el código del QR", description = """
			Marca al dueño del código leído por la cámara o el lector (o escrito a mano), con o sin el prefijo `ASIS:`. El \
			código se busca solo en la iglesia del token. Si ya estaba marcado responde 200 con `yaRegistrado = true`: no es un \
			error. Dos equipos que leen a la misma persona a la vez no duplican la marca. La respuesta trae el total para \
			actualizar el contador. Requiere `asistencia.registrar` o, en las reuniones de sus grupos, `asistencia.grupo`.""")
	@ApiResponse(responseCode = "201", description = "Miembro marcado.")
	@ApiResponse(responseCode = "200", description = "El miembro ya estaba marcado (`yaRegistrado = true`).")
	@ApiResponse(responseCode = "400", description = CODIGO_INVALIDO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CODIGO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = MIEMBRO_INACTIVO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<MarcaResponse> marcarPorCodigo(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id,
			@Valid @RequestBody MarcarCodigoRequest solicitud) {
		return responder(servicio.marcarPorCodigo(id, solicitud.codigo()));
	}

	@PostMapping("/asistencias")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Marcar a mano", description = """
			Marca a un miembro activo de la iglesia elegido a mano (en las reuniones de grupo puede ser de otro grupo). Mismas \
			respuestas que marcar con el código. Requiere `asistencia.registrar` o, en las reuniones de sus grupos, \
			`asistencia.grupo`.""")
	@ApiResponse(responseCode = "201", description = "Miembro marcado.")
	@ApiResponse(responseCode = "200", description = "El miembro ya estaba marcado (`yaRegistrado = true`).")
	@ApiResponse(responseCode = "400", description = MARCA_INVALIDA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = MIEMBRO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = MIEMBRO_INACTIVO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<MarcaResponse> marcarMiembro(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id,
			@Valid @RequestBody MarcarMiembroRequest solicitud) {
		return responder(servicio.marcarMiembro(id, solicitud.miembroId()));
	}

	@DeleteMapping("/asistencias/{miembroId}")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Quitar una asistencia", description = """
			Borra la marca de un miembro en la reunión (el «Deshacer» de la pantalla de escaneo). Requiere \
			`asistencia.registrar` o, en las reuniones de sus grupos, `asistencia.grupo`.""")
	@ApiResponse(responseCode = "204", description = "Asistencia quitada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_MARCADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> quitar(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id,
			@Parameter(description = "Identificador del miembro.", example = "12") @PathVariable Long miembroId) {
		servicio.quitar(id, miembroId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/asistentes")
	@PreAuthorize(Permisos.PUEDE_VER_ASISTENTES)
	@Operation(summary = "Asistentes de una reunión", description = """
			Página de los miembros marcados, los últimos primero, con su nivel, grupo, medio y hora; `q` filtra por nombre \
			(sin distinguir tildes ni mayúsculas). Orden admitido: `registradaEn`. Requiere `asistencia.ver`, \
			`asistencia.registrar` o `asistencia.grupo` (este, en las reuniones de sus grupos).""")
	@ApiResponse(responseCode = "200", description = "Página de asistentes.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<AsistenteResponse> asistentes(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id,
			@Parameter(description = "Texto contenido en los nombres o apellidos.", example = "torres") @RequestParam(required = false) String q,
			@ParameterObject @PageableDefault(sort = "registradaEn", direction = Sort.Direction.DESC) Pageable paginacion) {
		return servicio.asistentes(id, q, paginacion);
	}

	@GetMapping("/integrantes")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Integrantes de una reunión de grupo", description = """
			Lista para marcar a mano en una reunión de grupo: los integrantes activos del grupo con si están presentes y, al \
			final, quienes vinieron de otro grupo y ya están marcados (`integrante = false`). Solo para reuniones de grupo (las \
			demás responden 400). Requiere `asistencia.registrar` o, en las reuniones de sus grupos, `asistencia.grupo`.""")
	@ApiResponse(responseCode = "200", description = "Integrantes con su asistencia.")
	@ApiResponse(responseCode = "400", description = SIN_INTEGRANTES,
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
	List<MiembroReunionResponse> integrantes(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id) {
		return servicio.integrantes(id);
	}

	@GetMapping("/miembros")
	@PreAuthorize(Permisos.PUEDE_TOMAR_ASISTENCIA)
	@Operation(summary = "Buscar miembros para marcar a mano", description = """
			Hasta 20 miembros activos de la iglesia cuyo nombre contiene `q` (al menos 2 caracteres; con menos devuelve una \
			lista vacía), con su nivel y si ya están presentes en la reunión. Lo usa quien toma asistencia aunque no tenga \
			permiso para ver las fichas de los miembros. Requiere `asistencia.registrar` o, en las reuniones de sus grupos, \
			`asistencia.grupo`.""")
	@ApiResponse(responseCode = "200", description = "Miembros encontrados.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<MiembroReunionResponse> buscarMiembros(@Parameter(description = DESCRIPCION_ID, example = "120") @PathVariable Long id,
			@Parameter(description = "Texto contenido en los nombres o apellidos.", example = "ana") @RequestParam(required = false) String q) {
		return servicio.buscarMiembros(id, q);
	}

	private static ResponseEntity<MarcaResponse> responder(Marca marca) {
		return ResponseEntity.status(marca.nueva() ? HttpStatus.CREATED : HttpStatus.OK).body(marca.respuesta());
	}

}
