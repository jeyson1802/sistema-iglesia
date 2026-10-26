package com.iglesia.api.common.multiiglesia.component;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

/** Simula en pruebas la autenticación que deja el resource server tras validar un JWT. */
final class ContextoJwt {

	private ContextoJwt() {
	}

	static void autenticarConIglesia(Long iglesiaId) {
		Jwt.Builder jwt = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("1")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.claim(ClaimsJwt.ROLES, List.of("ADMIN_IGLESIA"));
		if (iglesiaId != null) {
			jwt.claim(ClaimsJwt.IGLESIA_ID, iglesiaId);
		}
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build()));
	}

}
