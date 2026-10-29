package com.iglesia.api.common.multiiglesia.component;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

/**
 * Iglesia (tenant) de la petición actual. Se toma exclusivamente del claim del JWT validado,
 * nunca de parámetros enviados por el cliente. El contexto lo limpia Spring Security al terminar la petición.
 */
@Component
public class IglesiaActual {

	static final String SIN_IGLESIA = "Esta operación se hace dentro de una iglesia: entre primero a operar una iglesia";

	public Optional<Long> id() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion instanceof JwtAuthenticationToken token
				&& token.getToken().getClaim(ClaimsJwt.IGLESIA_ID) instanceof Number iglesiaId) {
			return Optional.of(iglesiaId.longValue());
		}
		return Optional.empty();
	}

	/** Iglesia del token para las operaciones que solo tienen sentido dentro de una iglesia (400 si no hay). */
	public Long requerida() {
		return id().orElseThrow(() -> new ReglaNegocioException(SIN_IGLESIA));
	}

}
