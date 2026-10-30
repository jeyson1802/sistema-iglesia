package com.iglesia.api.consolidacion.controller;

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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.dto.AvanceRondaResponse;
import com.iglesia.api.consolidacion.dto.CasoResumenResponse;
import com.iglesia.api.consolidacion.dto.FiltroCasosRonda;
import com.iglesia.api.consolidacion.dto.FiltroLoteRequest;
import com.iglesia.api.consolidacion.dto.FiltroRondas;
import com.iglesia.api.consolidacion.dto.GuardarRondaRequest;
import com.iglesia.api.consolidacion.dto.LoteAgregadoResponse;
import com.iglesia.api.consolidacion.dto.PropuestaRepartoResponse;
import com.iglesia.api.consolidacion.dto.RepartirRequest;
import com.iglesia.api.consolidacion.dto.RondaResponse;
import com.iglesia.api.consolidacion.dto.VistaPreviaLoteResponse;
import com.iglesia.api.consolidacion.service.AvanceRondaService;
import com.iglesia.api.consolidacion.service.CasoService;
import com.iglesia.api.consolidacion.service.LoteService;
import com.iglesia.api.consolidacion.service.RepartoService;
import com.iglesia.api.consolidacion.service.RondaService;

@RestController
@RequestMapping("/api/consolidacion/rondas")
@Tag(name = "Consolidación: rondas", description = """
		Rondas de consolidación: un lote de personas que el equipo llama o visita en un período. Ciclo: planificada \
		(se arma el lote y se reparte) → en curso (los consolidadores ven sus casos) → cerrada (solo lectura; los \
		casos abiertos quedan como «No concluido»). Consultar exige `consolidacion.ver` o \
		`consolidacion.administrar`; modificar, `consolidacion.administrar`.""")
class RondaConsolidacionController {

	private static final String DESCRIPCION_ID = "Identificador de la ronda.";

	private static final String NO_ENCONTRADA = "La ronda no existe en la iglesia del token.";

	private static final String RONDA_CERRADA = "La ronda está cerrada (solo lectura).";

	private final RondaService rondas;

	private final LoteService lotes;

	private final RepartoService repartos;

	private final CasoService casos;

	private final AvanceRondaService avances;

	RondaConsolidacionController(RondaService rondas, LoteService lotes, RepartoService repartos, CasoService casos,
			AvanceRondaService avances) {
		this.rondas = rondas;
		this.lotes = lotes;
		this.repartos = repartos;
		this.casos = casos;
		this.avances = avances;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_RONDAS)
	@Operation(summary = "Listar rondas", description = """
			Rondas de la más reciente a la más antigua (por fecha de inicio), paginadas, cada una con el resumen de su avance: \
			personas, pendientes, en contacto, cerrados, sin asignar y con el próximo contacto vencido. Requiere \
			`consolidacion.ver` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Página de rondas.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<RondaResponse> listar(@ParameterObject FiltroRondas filtro, @ParameterObject @PageableDefault Pageable paginacion) {
		return rondas.listar(filtro, paginacion);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_RONDAS)
	@Operation(summary = "Obtener una ronda", description = """
			Datos de la ronda con el resumen de su avance. Requiere `consolidacion.ver` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Ronda.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RondaResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		return rondas.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Crear una ronda", description = """
			Crea una ronda planificada (vacía). Nombre único en la iglesia sin distinguir mayúsculas (409); la fecha de fin, si \
			se indica, igual o posterior a la de inicio (400). Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "201", description = "Ronda creada.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la ronda creada."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Ya existe otra ronda con ese nombre.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<RondaResponse> crear(@Valid @RequestBody GuardarRondaRequest solicitud) {
		RondaResponse creada = rondas.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Editar una ronda", description = """
			Cambia nombre, descripción y fechas de una ronda planificada o en curso. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Ronda actualizada.")
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
	@ApiResponse(responseCode = "409", description = "Nombre repetido o ronda cerrada.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RondaResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody GuardarRondaRequest solicitud) {
		return rondas.actualizar(id, solicitud);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Borrar una ronda", description = """
			Borra una ronda planificada junto con su lote (409 si ya empezó). Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Ronda borrada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "La ronda no está planificada.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void borrar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		rondas.borrar(id);
	}

	@PostMapping("/{id}/iniciar")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Iniciar una ronda", description = """
			Pasa la ronda de planificada a en curso: desde ahora cada consolidador ve sus casos en «Mis asignados». 409 si no \
			está planificada o si no tiene personas. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Ronda en curso.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Transición no válida o ronda vacía.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RondaResponse iniciar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		return rondas.iniciar(id);
	}

	@PostMapping("/{id}/cerrar")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Cerrar una ronda", description = """
			Cierra la ronda en curso: queda de solo lectura y los casos que seguían abiertos se cierran sin resultado \
			(«No concluido»). 409 si no está en curso. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Ronda cerrada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "La ronda no está en curso.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RondaResponse cerrar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		return rondas.cerrar(id);
	}

	@PostMapping("/{id}/lote/vista-previa")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Vista previa del lote", description = """
			Cuántas personas entrarían en la ronda con los filtros, cuántas quedan fuera por tener ya un seguimiento abierto y \
			el nombre completo de cada persona que se agregaría, por apellidos (hasta 1000, el tope de un lote). No guarda nada. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Vista previa del lote.")
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
	@ApiResponse(responseCode = "409", description = RONDA_CERRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	VistaPreviaLoteResponse vistaPreviaLote(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody FiltroLoteRequest filtro) {
		return lotes.vistaPrevia(id, filtro);
	}

	@PostMapping("/{id}/lote")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Agregar personas a la ronda", description = """
			Agrega en una sola operación las personas activas que cumplen los filtros (o las elegidas en `miembroIds`). Quien \
			ya tiene un seguimiento abierto no entra. Hasta 1000 personas por lote (400). Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Cuántas personas se agregaron.")
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
	@ApiResponse(responseCode = "409", description = RONDA_CERRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	LoteAgregadoResponse agregarLote(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody FiltroLoteRequest filtro) {
		return lotes.agregar(id, filtro);
	}

	@GetMapping("/{id}/casos")
	@PreAuthorize(Permisos.PUEDE_VER_RONDAS)
	@Operation(summary = "Casos de una ronda", description = """
			Casos de la ronda en el orden del lote (por apellidos), paginados: persona, consolidador, estado, contactos por \
			medio, último contacto, próximo contacto, resultado e iteración. Requiere `consolidacion.ver` o \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Página de casos.")
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
	Page<CasoResumenResponse> casos(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@ParameterObject FiltroCasosRonda filtro, @ParameterObject @PageableDefault Pageable paginacion) {
		return casos.deRonda(id, filtro, paginacion);
	}

	@DeleteMapping("/{id}/casos/{casoId}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Quitar a una persona del lote", description = """
			Quita el caso de la ronda mientras no tenga contactos (409 si ya tiene o si la ronda está cerrada). Requiere \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Persona quitada del lote.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "La ronda o el caso no existen.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El caso tiene contactos o la ronda está cerrada.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void quitar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Parameter(description = "Identificador del caso.", example = "310") @PathVariable Long casoId) {
		lotes.quitar(id, casoId);
	}

	@PostMapping("/{id}/repartir/vista-previa")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Vista previa del reparto", description = """
			Propone quién recibe a quién, balanceando por carga: cada caso va al consolidador con menos casos abiertos en ese \
			momento (contando los de otras rondas). No guarda nada. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Propuesta de reparto.")
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
	@ApiResponse(responseCode = "409", description = RONDA_CERRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	PropuestaRepartoResponse vistaPreviaReparto(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody RepartirRequest solicitud) {
		return repartos.vistaPrevia(id, solicitud);
	}

	@PostMapping("/{id}/repartir")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Repartir una ronda", description = """
			Guarda el reparto balanceado entre los consolidadores elegidos o, si se envían `asignaciones`, exactamente esas (la \
			propuesta ajustada a mano). Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Ronda con su nuevo resumen.")
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
	@ApiResponse(responseCode = "409", description = RONDA_CERRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RondaResponse repartir(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody RepartirRequest solicitud) {
		return repartos.repartir(id, solicitud);
	}

	@GetMapping("/{id}/avance")
	@PreAuthorize(Permisos.PUEDE_VER_RONDAS)
	@Operation(summary = "Avance de una ronda", description = """
			Embudo (personas, contactadas, volvieron a asistir en los 28 días siguientes a la primera conversación), estados, \
			reparto por resultado y avance por consolidador. Requiere `consolidacion.ver` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Indicadores de la ronda.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AvanceRondaResponse avance(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		return avances.avance(id);
	}

}
