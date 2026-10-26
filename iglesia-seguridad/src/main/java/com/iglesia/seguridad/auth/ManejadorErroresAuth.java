package com.iglesia.seguridad.auth;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Un token de refresco inválido responde 401 y además borra la cookie, para que el navegador no
 * siga enviando un valor que ya no sirve. Tiene prioridad sobre el manejador global.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ManejadorErroresAuth {

	private final CookieRefresco cookieRefresco;

	ManejadorErroresAuth(CookieRefresco cookieRefresco) {
		this.cookieRefresco = cookieRefresco;
	}

	@ExceptionHandler(TokenRefrescoInvalidoException.class)
	ResponseEntity<ProblemDetail> tokenRefrescoInvalido(TokenRefrescoInvalidoException ex) {
		// Mismo mensaje genérico que el resto de fallos de autenticación: no revelar el motivo.
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"Credenciales inválidas o sesión expirada");
		problema.setTitle("No autenticado");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.SET_COOKIE, cookieRefresco.borrar().toString())
			.body(problema);
	}

}
