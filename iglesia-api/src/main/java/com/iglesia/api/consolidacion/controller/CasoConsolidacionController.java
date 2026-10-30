package com.iglesia.api.consolidacion.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.iglesia.api.consolidacion.dto.AbrirSeguimientoRequest;
import com.iglesia.api.consolidacion.dto.AsignarConsolidadorRequest;
import com.iglesia.api.consolidacion.dto.CasoDetalleResponse;
import com.iglesia.api.consolidacion.dto.CasoResumenResponse;
import com.iglesia.api.consolidacion.dto.CerrarCasoRequest;
import com.iglesia.api.consolidacion.dto.ContactoResponse;
import com.iglesia.api.consolidacion.dto.FiltroMisCasos;
import com.iglesia.api.consolidacion.dto.ReasignarRequest;
import com.iglesia.api.consolidacion.dto.RegistrarContactoRequest;
import com.iglesia.api.consolidacion.dto.ResumenMisCasosResponse;
import com.iglesia.api.consolidacion.service.CasoService;
import com.iglesia.api.consolidacion.service.ContactoService;
import com.iglesia.api.consolidacion.service.RepartoService;

@RestController
@RequestMapping("/api/consolidacion")
@Tag(name = "Consolidación: casos y contactos", description = """
		Seguimiento de cada persona: «Mis asignados» del consolidador, detalle para contactar, seguimientos \
		individuales, contactos (llamadas, WhatsApp, visitas), cierre con resultado y reapertura. El consolidador \
		(`consolidacion.contactar`) trabaja sus casos; el coordinador (`consolidacion.administrar`) puede hacer todo \
		lo del consolidador sobre cualquier caso. Fuera del ámbito del usuario, 404.""")
class CasoConsolidacionController {

	private static final String DESCRIPCION_CASO = "Identificador del caso.";

	private static final String DESCRIPCION_CONTACTO = "Identificador del contacto.";

	private static final String CASO_NO_ENCONTRADO = "El caso no existe o está fuera del ámbito del usuario.";

	private static final String CONTACTO_NO_ENCONTRADO = "El contacto no existe o está fuera del ámbito del usuario.";

	private static final String NO_TRABAJABLE = "El caso está cerrado o su ronda no está en curso.";

	private final CasoService casos;

	private final ContactoService contactos;

	private final RepartoService repartos;

	CasoConsolidacionController(CasoService casos, ContactoService contactos, RepartoService repartos) {
		this.casos = casos;
		this.contactos = contactos;
		this.repartos = repartos;
	}

	@GetMapping("/mis-casos")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Mis asignados", description = """
			Casos abiertos asignados al miembro del token, de rondas en curso o individuales, por sección: `PARA_HOY` \
			(próximo contacto hoy o vencido, del más vencido), `SIN_CONTACTAR` (los que más esperan primero) y `EN_CONTACTO` \
			(por próximo contacto). Cada uno trae su iteración y el resultado anterior. Requiere `consolidacion.contactar` o \
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
	Page<CasoResumenResponse> misCasos(@Valid @ParameterObject FiltroMisCasos filtro, @ParameterObject @PageableDefault Pageable paginacion) {
		return casos.misCasos(filtro, paginacion);
	}

	@GetMapping("/mis-casos/resumen")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Resumen de mis asignados", description = """
			Cantidad de casos de cada sección y avance del consolidador (asignados y con cuántos ya habló). Requiere \
			`consolidacion.contactar` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Resumen.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResumenMisCasosResponse resumenMisCasos() {
		return casos.resumenMisCasos();
	}

	@GetMapping("/casos/{id}")
	@PreAuthorize(Permisos.PUEDE_CONSULTAR_CONSOLIDACION)
	@Operation(summary = "Detalle de un caso", description = """
			Datos de contacto de la persona, sus contactos, su asistencia (resumen y reuniones del período del caso), el \
			resumen «Antes de contactar» y sus iteraciones anteriores con resultado y observación. Indica si el usuario puede \
			trabajarlo y reasignarlo. Requiere cualquier permiso de consolidación, con ámbito.""")
	@ApiResponse(responseCode = "200", description = "Detalle del caso.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CASO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CasoDetalleResponse detalle(@Parameter(description = DESCRIPCION_CASO, example = "310") @PathVariable Long id) {
		return casos.detalle(id);
	}

	@PostMapping("/casos")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Iniciar un seguimiento individual", description = """
			Abre un caso sin ronda para una persona activa. El consolidador es quien lo abre; solo el coordinador puede elegir \
			a otro. El consolidador solo puede abrirlo a personas de su red (403). 409 si la persona ya tiene un seguimiento \
			abierto o está inactiva. Requiere `consolidacion.contactar` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "201", description = "Seguimiento abierto.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del caso."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "La persona no existe o está fuera del ámbito.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Ya tiene un seguimiento abierto o está inactiva.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<CasoDetalleResponse> abrir(@Valid @RequestBody AbrirSeguimientoRequest solicitud) {
		CasoDetalleResponse creado = casos.abrir(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/consolidacion/casos/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PostMapping("/casos/{id}/cerrar")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Cerrar un caso", description = """
			Cierra el caso con un resultado activo del catálogo y una observación opcional. El consolidador, sus casos; el \
			coordinador, cualquiera. Requiere `consolidacion.contactar` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Caso cerrado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CASO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = NO_TRABAJABLE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CasoDetalleResponse cerrar(@Parameter(description = DESCRIPCION_CASO, example = "310") @PathVariable Long id,
			@Valid @RequestBody CerrarCasoRequest solicitud) {
		return casos.cerrar(id, solicitud);
	}

	@PostMapping("/casos/{id}/reabrir")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Reabrir un caso", description = """
			Deshace el cierre (por error de registro) de un caso de una ronda en curso o individual: vuelve a «en contacto» o \
			a «pendiente». 409 si no está cerrado o si la persona ya tiene otro seguimiento abierto. Requiere \
			`consolidacion.contactar` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Caso reabierto.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CASO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El caso no está cerrado, su ronda no está en curso o hay otro seguimiento abierto.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CasoDetalleResponse reabrir(@Parameter(description = DESCRIPCION_CASO, example = "310") @PathVariable Long id) {
		return casos.reabrir(id);
	}

	@PostMapping("/casos/{id}/contactos")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Registrar un contacto", description = """
			Registra una llamada, mensaje, visita o conversación en la iglesia. La fecha no puede ser futura; el resultado \
			debe tener sentido para el medio; el próximo contacto, el mismo día o después. El primero pasa el caso a \
			«en contacto». Solo el coordinador puede registrar en nombre de otra persona. Requiere `consolidacion.contactar` \
			o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "201", description = "Contacto registrado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CASO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = NO_TRABAJABLE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.CREATED)
	ContactoResponse registrarContacto(@Parameter(description = DESCRIPCION_CASO, example = "310") @PathVariable Long id,
			@Valid @RequestBody RegistrarContactoRequest solicitud) {
		return contactos.registrar(id, solicitud);
	}

	@PutMapping("/contactos/{id}")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Corregir un contacto", description = """
			Quien lo registró puede corregirlo durante 24 horas (después, 409); el coordinador, siempre. Requiere \
			`consolidacion.contactar` o `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "200", description = "Contacto corregido.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CONTACTO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Pasaron 24 horas, o el caso no se puede trabajar.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ContactoResponse corregirContacto(@Parameter(description = DESCRIPCION_CONTACTO, example = "900") @PathVariable Long id,
			@Valid @RequestBody RegistrarContactoRequest solicitud) {
		return contactos.actualizar(id, solicitud);
	}

	@DeleteMapping("/contactos/{id}")
	@PreAuthorize(Permisos.PUEDE_CONTACTAR)
	@Operation(summary = "Borrar un contacto", description = """
			Mismas reglas que corregir. Si era el único, el caso vuelve a «pendiente». Requiere `consolidacion.contactar` o \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Contacto borrado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = CONTACTO_NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Pasaron 24 horas, o el caso no se puede trabajar.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void borrarContacto(@Parameter(description = DESCRIPCION_CONTACTO, example = "900") @PathVariable Long id) {
		contactos.borrar(id);
	}

	@PatchMapping("/casos/{id}/consolidador")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Reasignar un caso", description = """
			Cambia el consolidador de un caso abierto por otro miembro activo. Requiere `consolidacion.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Caso reasignado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El caso o el miembro no existen.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El miembro está inactivo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reasignar(@Parameter(description = DESCRIPCION_CASO, example = "310") @PathVariable Long id,
			@Valid @RequestBody AsignarConsolidadorRequest solicitud) {
		repartos.reasignar(List.of(id), solicitud.consolidadorId());
	}

	@PatchMapping("/casos/consolidador")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_CONSOLIDACION)
	@Operation(summary = "Reasignar varios casos", description = """
			Pasa varios casos abiertos a otro miembro activo (p. ej. los de un consolidador que deja el equipo). Requiere \
			`consolidacion.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Casos reasignados.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "Algún caso o el miembro no existen.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El miembro está inactivo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reasignarVarios(@Valid @RequestBody ReasignarRequest solicitud) {
		repartos.reasignar(solicitud.casoIds(), solicitud.consolidadorId());
	}

}
