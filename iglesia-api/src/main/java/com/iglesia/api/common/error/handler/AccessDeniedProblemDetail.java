package com.iglesia.api.common.error.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Responde los 403 de la cadena de seguridad con el mismo {@code ProblemDetail} y el mismo registro en el log
 * que el resto de errores: delega en el manejador global. Los 403 de {@code @PreAuthorize} llegan al manejador
 * global directamente.
 */
@Component
public class AccessDeniedProblemDetail implements AccessDeniedHandler {

	/** Solo agrega la cabecera {@code WWW-Authenticate} que exige la RFC 6750; no escribe cuerpo. */
	private final BearerTokenAccessDeniedHandler cabeceraBearer = new BearerTokenAccessDeniedHandler();

	private final HandlerExceptionResolver resolutorExcepciones;

	public AccessDeniedProblemDetail(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutorExcepciones) {
		this.resolutorExcepciones = resolutorExcepciones;
	}

	@Override
	public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException ex) {
		cabeceraBearer.handle(peticion, respuesta, ex);
		resolutorExcepciones.resolveException(peticion, respuesta, null, ex);
	}

}
