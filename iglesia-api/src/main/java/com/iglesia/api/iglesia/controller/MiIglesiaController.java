package com.iglesia.api.iglesia.controller;

import java.io.InputStream;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.service.MiIglesiaService;

@RestController
@RequestMapping("/api/mi-iglesia")
@Tag(name = "Mi iglesia", description = """
		Identidad y apariencia de la iglesia en la que se opera, tomada siempre del token (nunca de un parámetro). \
		Cualquier usuario autenticado de la iglesia la lee: la usan la topbar, el footer y el tema de todos. \
		Editarla exige el permiso `iglesia.personalizar` (administrador de la iglesia). Sin iglesia en el token \
		(administrador del sistema que no entró a ninguna) responde 400.""")
class MiIglesiaController {

	private static final String SIN_IGLESIA = "El token no tiene iglesia: primero hay que entrar a operar una.";

	private static final String DATOS_INVALIDOS_O_SIN_IGLESIA = RespuestasOpenApi.DATOS_INVALIDOS + " También si "
			+ "el token no tiene iglesia.";

	private static final String NOMBRE_REPETIDO = "Ya existe otra iglesia con ese nombre comercial (sin distinguir "
			+ "mayúsculas).";

	private final MiIglesiaService servicio;

	MiIglesiaController(MiIglesiaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Obtener mi iglesia", description = """
			Identidad (nombres, dirección, contacto, si tiene logo) y apariencia de la iglesia del token. Basta con \
			estar autenticado dentro de una iglesia.""")
	@ApiResponse(responseCode = "200", description = "Identidad de la iglesia.")
	@ApiResponse(responseCode = "400", description = SIN_IGLESIA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	IglesiaResponse obtener() {
		return servicio.obtener();
	}

	@PutMapping
	@PreAuthorize(Permisos.PUEDE_PERSONALIZAR_IGLESIA)
	@Operation(summary = "Editar mi iglesia", description = """
			Cambia los nombres, la dirección y el contacto de la iglesia del token. Requiere el permiso \
			`iglesia.personalizar`. Mismas reglas que el alta del catálogo: nombre comercial de 2 a 60 caracteres y \
			único, denominación, dirección y distrito obligatorios; teléfono, correo y sitio web opcionales. No \
			puede activar ni desactivar la iglesia.""")
	@ApiResponse(responseCode = "200", description = "Iglesia actualizada.")
	@ApiResponse(responseCode = "400", description = DATOS_INVALIDOS_O_SIN_IGLESIA,
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
	IglesiaResponse actualizar(@Valid @RequestBody GuardarIglesiaRequest solicitud) {
		return servicio.actualizar(solicitud);
	}

	@PutMapping("/apariencia")
	@PreAuthorize(Permisos.PUEDE_PERSONALIZAR_IGLESIA)
	@Operation(summary = "Cambiar la apariencia de mi iglesia", description = """
			Preset, color primario, superficie y tipografía que verán todos los usuarios de la iglesia al iniciar \
			sesión. Requiere el permiso `iglesia.personalizar`. Un valor fuera de los admitidos responde 400 con el \
			error junto a su campo.""")
	@ApiResponse(responseCode = "200", description = "Apariencia guardada.")
	@ApiResponse(responseCode = "400", description = DATOS_INVALIDOS_O_SIN_IGLESIA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AparienciaResponse cambiarApariencia(@Valid @RequestBody GuardarAparienciaRequest solicitud) {
		return servicio.cambiarApariencia(solicitud);
	}

	@GetMapping("/logo")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Obtener el logo de mi iglesia", description = """
			Imagen del logo de la iglesia del token. Basta con estar autenticado dentro de una iglesia. Se pide con \
			el token (no es una URL pública) y admite `If-None-Match`.""")
	@ApiResponse(responseCode = "200", description = DocumentacionLogo.LOGO, content = {
			@Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")),
			@Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")) })
	@ApiResponse(responseCode = "304", description = "La versión del cliente sigue vigente.")
	@ApiResponse(responseCode = "400", description = SIN_IGLESIA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = DocumentacionLogo.SIN_LOGO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<byte[]> logo(WebRequest peticion) {
		return RespuestaLogo.de(servicio.logo(), peticion);
	}

	@PutMapping("/logo")
	@PreAuthorize(Permisos.PUEDE_PERSONALIZAR_IGLESIA)
	@Operation(summary = "Subir el logo de mi iglesia", description = DocumentacionLogo.CUERPO + """

			Reemplaza el anterior. Requiere el permiso `iglesia.personalizar`.""")
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
	@ApiResponse(responseCode = "413", description = DocumentacionLogo.DEMASIADO_GRANDE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> subirLogo(@Parameter(hidden = true) InputStream contenido) {
		servicio.guardarLogo(contenido);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/logo")
	@PreAuthorize(Permisos.PUEDE_PERSONALIZAR_IGLESIA)
	@Operation(summary = "Quitar el logo de mi iglesia", description = """
			Borra el logo; la interfaz vuelve a mostrar el monograma con las iniciales. Requiere el permiso \
			`iglesia.personalizar`.""")
	@ApiResponse(responseCode = "204", description = "Logo quitado (o la iglesia no tenía).")
	@ApiResponse(responseCode = "400", description = SIN_IGLESIA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> quitarLogo() {
		servicio.quitarLogo();
		return ResponseEntity.noContent().build();
	}

}
