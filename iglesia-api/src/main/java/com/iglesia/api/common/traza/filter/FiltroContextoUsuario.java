package com.iglesia.api.common.traza.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

/**
 * Agrega al MDC el {@code usuarioId} (claim {@code sub}) y el {@code iglesiaId} del JWT validado, para que cada
 * línea de log de la petición diga quién la hizo. Nunca agrega correo, nombre ni otros datos personales.
 * <p>
 * No es un {@code @Component}: se registra dentro de la cadena de Spring Security, después de la autenticación
 * por token. Como filtro de servlet independiente correría antes y no vería al usuario.
 */
public class FiltroContextoUsuario extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
			cadena.doFilter(peticion, respuesta);
			return;
		}
		Jwt jwt = token.getToken();
		MDC.put(TrazaConstantes.MDC_USUARIO_ID, jwt.getSubject());
		Object iglesiaId = jwt.getClaim(ClaimsJwt.IGLESIA_ID);
		if (iglesiaId != null) {
			MDC.put(TrazaConstantes.MDC_IGLESIA_ID, iglesiaId.toString());
		}
		try {
			cadena.doFilter(peticion, respuesta);
		}
		finally {
			// El hilo vuelve al pool: la siguiente petición no debe heredar el usuario de esta.
			MDC.remove(TrazaConstantes.MDC_USUARIO_ID);
			MDC.remove(TrazaConstantes.MDC_IGLESIA_ID);
		}
	}

}
