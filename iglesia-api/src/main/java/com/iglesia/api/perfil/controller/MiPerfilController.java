package com.iglesia.api.perfil.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.CodigoAsistenciaResponse;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.perfil.dto.MiPerfilResponse;
import com.iglesia.api.perfil.service.MiPerfilService;

@RestController
@RequestMapping("/api/mi-perfil")
@Tag(name = "Mi perfil", description = """
		Portal del miembro: su propia información (datos, asistencia y código QR). Basta estar autenticado y \
		vinculado a un miembro; no exige permisos de módulo porque el miembro sale solo del claim `miembro_id` del \
		token, nunca de un parámetro, y no hay forma de pedir datos de otra persona. Nunca devuelve aportes ni \
		consolidación.""")
class MiPerfilController {

	private static final String SIN_VINCULO = "El usuario no está vinculado a un miembro de la iglesia (o el miembro ya "
			+ "no existe). Se devuelve como `ProblemDetail`.";

	private final MiPerfilService servicio;

	MiPerfilController(MiPerfilService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Mis datos", description = """
			Nombre, edad calculada, fecha de ingreso, condición, nivel, líder y grupo (con su día y hora de reunión) \
			del miembro vinculado al usuario.""")
	@ApiResponse(responseCode = "200", description = "Datos del propio miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = SIN_VINCULO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiPerfilResponse datos() {
		return servicio.datos();
	}

	@GetMapping("/asistencia")
	@Operation(summary = "Mi asistencia", description = """
			Los mismos indicadores de la ficha del miembro (porcentaje de las últimas 8 semanas, faltas seguidas, \
			última asistencia y alertas), su asistencia por semana y sus últimas reuniones asistidas.""")
	@ApiResponse(responseCode = "200", description = "Asistencia del propio miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = SIN_VINCULO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	AsistenciaMiembroResponse asistencia() {
		return servicio.asistencia();
	}

	@GetMapping("/codigo-asistencia")
	@Operation(summary = "Mi código de asistencia", description = """
			Código personal y contenido del QR con que el miembro marca su asistencia (el mismo del fotocheck). Si \
			aún no tiene código, se genera en ese momento. El frontend dibuja el QR; el backend no entrega imágenes.""")
	@ApiResponse(responseCode = "200", description = "Código del propio miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = SIN_VINCULO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	CodigoAsistenciaResponse codigoAsistencia() {
		return servicio.codigoAsistencia();
	}

}
