package com.iglesia.seguridad.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private static final String COOKIE_REFRESCO = "${seguridad.cookie-refresco.nombre}";

	private final AuthService servicio;

	private final CookieRefresco cookieRefresco;

	AuthController(AuthService servicio, CookieRefresco cookieRefresco) {
		this.servicio = servicio;
		this.cookieRefresco = cookieRefresco;
	}

	@PostMapping("/login")
	ResponseEntity<TokenAccesoResponse> iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		return responder(servicio.iniciarSesion(solicitud));
	}

	/** Sin cookie o con un token inválido responde 401 y borra la cookie (ver {@link ManejadorErroresAuth}). */
	@PostMapping("/refresh")
	ResponseEntity<TokenAccesoResponse> refrescar(
			@CookieValue(name = COOKIE_REFRESCO, required = false) String tokenRefresco) {
		if (tokenRefresco == null) {
			throw new TokenRefrescoInvalidoException();
		}
		return responder(servicio.refrescar(tokenRefresco));
	}

	/** Siempre responde 204 y borra la cookie, haya o no un token que revocar. */
	@PostMapping("/logout")
	ResponseEntity<Void> cerrarSesion(@CookieValue(name = COOKIE_REFRESCO, required = false) String tokenRefresco) {
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
