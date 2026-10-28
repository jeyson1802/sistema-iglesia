package com.iglesia.seguridad.common.seguridad.constant;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Únicas rutas protegidas que se permiten mientras el usuario tiene una contraseña temporal: consultar su
 * propio usuario y cambiar su contraseña. Todo lo demás se bloquea por defecto, incluido cualquier endpoint
 * nuevo. Las rutas públicas no pasan por la regla porque no llevan token.
 */
public final class RutasCambioContrasena {

	public static final RequestMatcher PERMITIDAS = new OrRequestMatcher(
			PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/usuarios/me"),
			PathPatternRequestMatcher.pathPattern(HttpMethod.PUT, "/api/usuarios/me/contrasena"));

	private RutasCambioContrasena() {
	}

}
