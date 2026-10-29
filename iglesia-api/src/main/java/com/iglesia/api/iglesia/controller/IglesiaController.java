package com.iglesia.api.iglesia.controller;

import java.io.InputStream;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.FiltroIglesias;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.service.IglesiaService;
import com.iglesia.api.iglesia.service.LogoIglesiaService;

@RestController
@RequestMapping("/api/iglesias")
@Tag(name = "Iglesias", description = """
		Catálogo de iglesias (tenants) del sistema con su identidad (nombres, dirección, contacto, logo) y su \
		apariencia. Solo lo usa el administrador del sistema: consultar exige el permiso `iglesias.ver` y dar de \
		alta o editar, `iglesias.administrar`. Dar de alta una iglesia es insertar una fila; su primer administrador \
		se crea después en iglesia-seguridad (`POST /api/usuarios` con este `iglesiaId`). El catálogo no se filtra \
		por la iglesia del token. Los usuarios de una iglesia leen y personalizan la suya en «Mi iglesia».""")
class IglesiaController {

	private static final String DESCRIPCION_ID = "Identificador de la iglesia.";

	private static final String NO_ENCONTRADA = "La iglesia no existe.";

	private static final String NOMBRE_REPETIDO = "Ya existe otra iglesia con ese nombre comercial (sin distinguir "
			+ "mayúsculas).";

	private final IglesiaService servicio;

	private final LogoIglesiaService logos;

	IglesiaController(IglesiaService servicio, LogoIglesiaService logos) {
		this.servicio = servicio;
		this.logos = logos;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_IGLESIAS)
	@Operation(summary = "Listar las iglesias", description = """
			Página del catálogo, por defecto ordenada por nombre comercial, con filtros opcionales por texto \
			(nombre comercial o denominación), ubigeo (departamento, provincia o distrito) y estado. Orden \
			admitido: `nombreComercial`, `denominacion`, `creadaEn`. Requiere el permiso `iglesias.ver`.""")
	@ApiResponse(responseCode = "200", description = "Página de iglesias.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<IglesiaResponse> listar(@Valid @ParameterObject FiltroIglesias filtro,
			@ParameterObject @PageableDefault(sort = "nombreComercial") Pageable paginacion) {
		return servicio.listar(filtro, paginacion);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_IGLESIAS)
	@Operation(summary = "Obtener una iglesia", description = """
			Identidad y apariencia de una iglesia del catálogo. Requiere el permiso `iglesias.ver`.""")
	@ApiResponse(responseCode = "200", description = "Iglesia encontrada.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	IglesiaResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Dar de alta una iglesia", description = """
			Agrega una iglesia al catálogo. Requiere el permiso `iglesias.administrar`.

			Reglas: nombre comercial (2 a 60 caracteres, único sin distinguir mayúsculas), denominación, dirección \
			y distrito (código INEI existente) son obligatorios; teléfono, correo y sitio web, opcionales. Los textos \
			se guardan sin espacios sobrantes. Nace con la apariencia por defecto (Aura, emerald, Playfair Display), \
			sin logo y sin usuarios: su primer administrador se crea en iglesia-seguridad con `POST /api/usuarios`, \
			indicando el `id` devuelto como `iglesiaId`.""")
	@ApiResponse(responseCode = "201", description = "Iglesia creada.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL de la iglesia creada."))
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
	ResponseEntity<IglesiaResponse> crear(@Valid @RequestBody GuardarIglesiaRequest solicitud) {
		IglesiaResponse creada = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creada.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creada);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Editar una iglesia", description = """
			Cambia la identidad de una iglesia (nombres, dirección y contacto). Requiere el permiso \
			`iglesias.administrar`. Mismas reglas que al crear; se pueden cambiar solo las mayúsculas del nombre \
			comercial actual.""")
	@ApiResponse(responseCode = "200", description = "Iglesia actualizada.")
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
	IglesiaResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Valid @RequestBody GuardarIglesiaRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PutMapping("/{id}/apariencia")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Cambiar la apariencia de una iglesia", description = """
			Preset, color primario, superficie y tipografía que verán todos los usuarios de la iglesia. Requiere el \
			permiso `iglesias.administrar`. Un valor fuera de los admitidos responde 400 con el error junto a su \
			campo.""")
	@ApiResponse(responseCode = "200", description = "Apariencia guardada.")
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
	AparienciaResponse cambiarApariencia(
			@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Valid @RequestBody GuardarAparienciaRequest solicitud) {
		return servicio.cambiarApariencia(id, solicitud);
	}

	@GetMapping("/{id}/logo")
	@PreAuthorize(Permisos.PUEDE_VER_IGLESIAS)
	@Operation(summary = "Obtener el logo de una iglesia", description = """
			Imagen del logo de una iglesia del catálogo (para sus miniaturas). Requiere el permiso `iglesias.ver`.""")
	@ApiResponse(responseCode = "200", description = DocumentacionLogo.LOGO, content = {
			@Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")) })
	@ApiResponse(responseCode = "304", description = "La versión del cliente sigue vigente.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = DocumentacionLogo.SIN_LOGO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<byte[]> logo(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			WebRequest peticion) {
		return RespuestaLogo.de(logos.obtener(id), peticion);
	}

	@PutMapping("/{id}/logo")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Subir el logo de una iglesia", description = DocumentacionLogo.CUERPO + """

			Reemplaza el anterior. Requiere el permiso `iglesias.administrar`.""")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = {
			@Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")) })
	@ApiResponse(responseCode = "204", description = "Logo guardado.")
	@ApiResponse(responseCode = "400", description = DocumentacionLogo.INVALIDO,
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
	@ApiResponse(responseCode = "413", description = DocumentacionLogo.DEMASIADO_GRANDE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> subirLogo(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id,
			@Parameter(hidden = true) InputStream contenido) {
		logos.guardar(id, contenido);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/logo")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@Operation(summary = "Quitar el logo de una iglesia", description = """
			Borra el logo; la interfaz vuelve a mostrar el monograma con las iniciales. Requiere el permiso \
			`iglesias.administrar`.""")
	@ApiResponse(responseCode = "204", description = "Logo quitado (o la iglesia no tenía).")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> quitarLogo(@Parameter(description = DESCRIPCION_ID, example = "1") @PathVariable Long id) {
		logos.quitar(id);
		return ResponseEntity.noContent().build();
	}

}
