package com.iglesia.seguridad.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.auth.component.CookieRefresco;
import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;
import com.iglesia.seguridad.auth.exception.TokenRefrescoInvalidoException;
import com.iglesia.seguridad.auth.model.SesionEmitida;
import com.iglesia.seguridad.auth.service.AuthService;
import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = """
		Inicio de sesión, renovación del token de acceso y cierre de sesión. Son endpoints públicos (sin token). \
		El token de acceso dura poco y viaja en el cuerpo; el de refresco va en una cookie HttpOnly, nunca \
		accesible desde JavaScript, y rota en cada uso.""")
@SecurityRequirements
public class AuthController {

	private static final String COOKIE_REFRESCO = "${seguridad.cookie-refresco.nombre}";

	private static final String DESCRIPCION_COOKIE = "Cookie HttpOnly con el token de refresco (por defecto "
			+ "`iglesia_refresco`). La envía el navegador automáticamente con `credentials: 'include'`.";

	private static final String SET_COOKIE_EMITIDA = "Emite la cookie HttpOnly con el nuevo token de refresco "
			+ "(Secure, SameSite=Strict, limitada a la ruta /api/auth).";

	private final AuthService servicio;

	private final CookieRefresco cookieRefresco;

	AuthController(AuthService servicio, CookieRefresco cookieRefresco) {
		this.servicio = servicio;
		this.cookieRefresco = cookieRefresco;
	}

	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión", description = """
			Valida correo y contraseña y devuelve un token de acceso (JWT RS256) con los roles, la iglesia y el \
			miembro vinculado del usuario. Además deja el token de refresco en una cookie HttpOnly.

			Por seguridad, un correo inexistente, una contraseña incorrecta o un usuario inactivo responden \
			igual (401), sin revelar el motivo.""")
	@ApiResponse(responseCode = "200", description = "Sesión iniciada.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = SET_COOKIE_EMITIDA))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<TokenAccesoResponse> iniciarSesion(@Valid @RequestBody LoginRequest solicitud,
			HttpServletRequest peticion) {
		// getRemoteAddr ya es la IP real del cliente gracias a server.forward-headers-strategy=framework.
		return responder(servicio.iniciarSesion(solicitud, peticion.getRemoteAddr()));
	}

	/** Sin cookie o con un token inválido responde 401 y borra la cookie (ver {@link ManejadorErroresAuth}). */
	@PostMapping("/refresh")
	@Operation(summary = "Renovar el token de acceso", description = """
			Usa el token de refresco de la cookie para emitir un token de acceso nuevo y rota el token de \
			refresco: el usado queda revocado y se emite otro en la cookie.

			Si se presenta un token de refresco ya usado, se asume robo y se cierran todas las sesiones del \
			usuario. Sin cookie, con un token inválido, expirado o revocado, o con el usuario inactivo, responde \
			401 y borra la cookie.""")
	@ApiResponse(responseCode = "200", description = "Token renovado.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = SET_COOKIE_EMITIDA))
	@ApiResponse(responseCode = "401", description = "Token de refresco ausente, inválido, expirado o revocado. "
			+ "La respuesta borra la cookie; hay que iniciar sesión de nuevo.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = "Borra la cookie del token de refresco."),
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<TokenAccesoResponse> refrescar(@Parameter(description = DESCRIPCION_COOKIE) @CookieValue(
			name = COOKIE_REFRESCO, required = false) String tokenRefresco) {
		if (tokenRefresco == null) {
			throw new TokenRefrescoInvalidoException();
		}
		return responder(servicio.refrescar(tokenRefresco));
	}

	/** Siempre responde 204 y borra la cookie, haya o no un token que revocar. */
	@PostMapping("/logout")
	@Operation(summary = "Cerrar sesión", description = """
			Revoca el token de refresco de la cookie y la borra. Es idempotente: responde 204 aunque no haya \
			cookie o el token ya no sea válido. El token de acceso sigue siendo válido hasta que expire, por eso \
			el cliente debe descartarlo.""")
	@ApiResponse(responseCode = "204", description = "Sesión cerrada.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = "Borra la cookie del token de refresco."))
	ResponseEntity<Void> cerrarSesion(@Parameter(description = DESCRIPCION_COOKIE) @CookieValue(
			name = COOKIE_REFRESCO, required = false) String tokenRefresco) {
		if (tokenRefresco != null) {
			servicio.cerrarSesion(tokenRefresco);
		}
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookieRefresco.borrar().toString()).build();
	}

	private ResponseEntity<TokenAccesoResponse> responder(SesionEmitida sesion) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieRefresco.emitir(sesion.tokenRefresco()).toString())
			.body(sesion.acceso());
	}

}
