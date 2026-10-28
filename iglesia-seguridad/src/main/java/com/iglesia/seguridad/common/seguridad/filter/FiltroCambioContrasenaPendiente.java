package com.iglesia.seguridad.common.seguridad.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iglesia.seguridad.common.seguridad.constant.RutasCambioContrasena;
import com.iglesia.seguridad.common.seguridad.exception.CambioContrasenaPendienteException;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

/**
 * Responde 403 a toda petición con un JWT que trae {@code cambio_contrasena: true}, salvo las rutas de
 * {@link RutasCambioContrasena#PERMITIDAS}. Es una regla transversal: un endpoint nuevo queda protegido sin
 * tener que recordarla en cada {@code @PreAuthorize}. Si el claim falta, se trata como {@code false}.
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
		if (debeCambiarContrasena() && !RutasCambioContrasena.PERMITIDAS.matches(peticion)) {
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
