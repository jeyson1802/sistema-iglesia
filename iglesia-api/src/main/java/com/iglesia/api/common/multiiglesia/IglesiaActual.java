package com.iglesia.api.common.multiiglesia;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.seguridad.ClaimsJwt;

/**
 * Iglesia (tenant) de la petición actual. Se toma exclusivamente del claim del JWT validado,
 * nunca de parámetros enviados por el cliente. El contexto lo limpia Spring Security al terminar la petición.
 */
@Component
public class IglesiaActual {

	public Optional<Long> id() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion instanceof JwtAuthenticationToken token
				&& token.getToken().getClaim(ClaimsJwt.IGLESIA_ID) instanceof Number iglesiaId) {
			return Optional.of(iglesiaId.longValue());
		}
		return Optional.empty();
	}

}
