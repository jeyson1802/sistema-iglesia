package com.iglesia.api.common.seguridad.component;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Usuario de la petición actual: el claim {@code sub} del JWT validado (id del usuario en iglesia-seguridad). Sirve
 * para registrar quién hace cada cambio sin datos personales.
 */
@Component
public class UsuarioActual {

	public Optional<Long> id() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion instanceof JwtAuthenticationToken token) {
			return Optional.of(Long.valueOf(token.getToken().getSubject()));
		}
		return Optional.empty();
	}

}
