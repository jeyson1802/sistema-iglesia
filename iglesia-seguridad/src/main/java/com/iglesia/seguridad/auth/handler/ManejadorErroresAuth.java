package com.iglesia.seguridad.auth.handler;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.iglesia.seguridad.auth.component.CookieRefresco;
import com.iglesia.seguridad.auth.controller.AuthController;
import com.iglesia.seguridad.auth.exception.TokenRefrescoInvalidoException;
import com.iglesia.seguridad.common.error.component.FabricaProblemDetail;

/**
 * Un token de refresco inválido responde 401 y además borra la cookie, para que el navegador no
 * siga enviando un valor que ya no sirve. Tiene prioridad sobre el manejador global.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ManejadorErroresAuth {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresAuth.class);

	private final CookieRefresco cookieRefresco;

	private final FabricaProblemDetail fabrica;

	ManejadorErroresAuth(CookieRefresco cookieRefresco, FabricaProblemDetail fabrica) {
		this.cookieRefresco = cookieRefresco;
		this.fabrica = fabrica;
	}

	@ExceptionHandler(TokenRefrescoInvalidoException.class)
	ResponseEntity<ProblemDetail> tokenRefrescoInvalido(TokenRefrescoInvalidoException ex, HttpServletRequest peticion) {
		// Mismo mensaje genérico que el resto de fallos de autenticación: no revelar el motivo.
		ProblemDetail problema = fabrica.noAutenticado();
		LOG.warn("{} {} {}: {}", problema.getStatus(), peticion.getMethod(), peticion.getRequestURI(),
				problema.getDetail());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.SET_COOKIE, cookieRefresco.borrar().toString())
			.body(problema);
	}

}
