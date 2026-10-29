package com.iglesia.seguridad.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.auth.component.CookieRefresco;
import com.iglesia.seguridad.auth.dto.EntrarAIglesiaRequest;
import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;
import com.iglesia.seguridad.auth.exception.TokenRefrescoInvalidoException;
import com.iglesia.seguridad.auth.model.SesionEmitida;
import com.iglesia.seguridad.auth.service.AuthService;
import com.iglesia.seguridad.common.openapi.config.OpenApiConfig;
import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = """
		Inicio de sesión, renovación del token de acceso y cierre de sesión, que son públicos (sin token), y \
		entrada y salida del administrador del sistema a una iglesia, que exigen token. El token de acceso dura \
		poco y viaja en el cuerpo; el de refresco va en una cookie HttpOnly, nunca accesible desde JavaScript, y \
		rota en cada uso.""")
@SecurityRequirements
public class AuthController {

	private static final String COOKIE_REFRESCO = "${seguridad.cookie-refresco.nombre}";

	private static final String DESCRIPCION_COOKIE = "Cookie HttpOnly con el token de refresco (por defecto "
			+ "`iglesia_refresco`). La envía el navegador automáticamente con `credentials: 'include'`.";

	private static final String SET_COOKIE_EMITIDA = "Emite la cookie HttpOnly con el nuevo token de refresco "
			+ "(Secure, SameSite=Strict, limitada a la ruta /api/auth).";

	private static final String NO_AUTENTICADO_CON_COOKIE = RespuestasOpenApi.NO_AUTENTICADO
			+ " También si falta la cookie de refresco, es inválida, expiró, fue revocada o es de otro usuario: en ese "
			+ "caso la respuesta borra la cookie.";

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
		return responder(servicio.refrescar(exigir(tokenRefresco)));
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

	@PostMapping("/iglesia-actuando")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
	@Operation(summary = "Entrar a una iglesia", description = """
			El administrador del sistema pasa a operar dentro de una iglesia del catálogo de iglesia-api. Requiere \
			el permiso `iglesias.administrar` y la cookie de refresco.

			Rota el token de refresco (el usado queda revocado; el nuevo recuerda la iglesia, así que renovar el \
			token la conserva) y devuelve un token de acceso con `iglesia_id` de esa iglesia, \
			`actuando_en_iglesia: true`, el rol `ADMIN_SISTEMA` y, sumados a los suyos, los permisos de un \
			administrador de iglesia. Con él, ambos backends aplican el aislamiento de esa iglesia. Si ya operaba \
			en otra iglesia, cambia a la nueva. La existencia de la iglesia no se valida aquí.""")
	@ApiResponse(responseCode = "200", description = "Token de acceso dentro de la iglesia.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = SET_COOKIE_EMITIDA))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = NO_AUTENTICADO_CON_COOKIE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<TokenAccesoResponse> entrarAIglesia(@Valid @RequestBody EntrarAIglesiaRequest solicitud,
			@Parameter(description = DESCRIPCION_COOKIE) @CookieValue(name = COOKIE_REFRESCO,
					required = false) String tokenRefresco) {
		return responder(servicio.entrarAIglesia(exigir(tokenRefresco), solicitud.iglesiaId()));
	}

	@DeleteMapping("/iglesia-actuando")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_IGLESIAS)
	@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
	@Operation(summary = "Salir de la iglesia", description = """
			El administrador del sistema vuelve a la vista global. Requiere el permiso `iglesias.administrar` y \
			la cookie de refresco.

			Rota el token de refresco sin iglesia y devuelve un token de acceso sin `iglesia_id` ni \
			`actuando_en_iglesia`, solo con sus propios permisos. Si no operaba en ninguna iglesia, responde igual.""")
	@ApiResponse(responseCode = "200", description = "Token de acceso de la vista global.",
			headers = @Header(name = HttpHeaders.SET_COOKIE, description = SET_COOKIE_EMITIDA))
	@ApiResponse(responseCode = "401", description = NO_AUTENTICADO_CON_COOKIE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<TokenAccesoResponse> salirDeIglesia(@Parameter(description = DESCRIPCION_COOKIE) @CookieValue(
			name = COOKIE_REFRESCO, required = false) String tokenRefresco) {
		return responder(servicio.salirDeIglesia(exigir(tokenRefresco)));
	}

	private static String exigir(String tokenRefresco) {
		if (tokenRefresco == null) {
			throw new TokenRefrescoInvalidoException();
		}
		return tokenRefresco;
	}

	private ResponseEntity<TokenAccesoResponse> responder(SesionEmitida sesion) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieRefresco.emitir(sesion.tokenRefresco()).toString())
			.body(sesion.acceso());
	}

}
