package com.iglesia.api.common.error.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Responde los 401 de la cadena de seguridad (sin token, token inválido o expirado) con el mismo
 * {@code ProblemDetail} y el mismo registro en el log que el resto de errores: delega en el manejador global.
 */
@Component
public class EntryPointProblemDetail implements AuthenticationEntryPoint {

	/** Solo agrega la cabecera {@code WWW-Authenticate: Bearer} que exige la RFC 6750; no escribe cuerpo. */
	private final BearerTokenAuthenticationEntryPoint cabeceraBearer = new BearerTokenAuthenticationEntryPoint();

	private final HandlerExceptionResolver resolutorExcepciones;

	public EntryPointProblemDetail(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutorExcepciones) {
		this.resolutorExcepciones = resolutorExcepciones;
	}

	@Override
	public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException ex) {
		cabeceraBearer.commence(peticion, respuesta, ex);
		resolutorExcepciones.resolveException(peticion, respuesta, null, ex);
	}

}
