package com.iglesia.api.common.seguridad.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.exception.CambioContrasenaPendienteException;

/**
 * Responde 403 a toda petición con un JWT que trae {@code cambio_contrasena: true}. A diferencia de
 * iglesia-seguridad no hay rutas permitidas: la contraseña se cambia allí, no en esta API. Si el claim falta, se
 * trata como {@code false}.
 * <p>
 * No es un {@code @Component}: se registra dentro de la cadena de Spring Security, después de la autenticación
 * por token, y responde con el mismo {@code ProblemDetail} que cualquier otro 403.
 */
public class FiltroCambioContrasenaPendiente extends OncePerRequestFilter {

	private final AccessDeniedHandler accesoDenegado;

	public FiltroCambioContrasenaPendiente(AccessDeniedHandler accesoDenegado) {
		this.accesoDenegado = accesoDenegado;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (debeCambiarContrasena()) {
			accesoDenegado.handle(peticion, respuesta, new CambioContrasenaPendienteException());
			return;
		}
		cadena.doFilter(peticion, respuesta);
	}

	private static boolean debeCambiarContrasena() {
		return SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token
				&& Boolean.TRUE.equals(token.getToken().getClaimAsBoolean(ClaimsJwt.CAMBIO_CONTRASENA));
	}

}
