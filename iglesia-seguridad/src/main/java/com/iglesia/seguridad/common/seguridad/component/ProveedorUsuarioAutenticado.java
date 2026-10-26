package com.iglesia.seguridad.common.seguridad;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Obtiene el usuario de la petición actual desde el contexto de Spring Security. */
@Component
public class ProveedorUsuarioAutenticado {

	public UsuarioAutenticado actual() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion instanceof JwtAuthenticationToken token) {
			return UsuarioAutenticado.desde(token.getToken());
		}
		throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado con JWT");
	}

}
