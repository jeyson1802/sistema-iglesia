package com.iglesia.api.common.seguridad.component;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioActualTest {

	private final UsuarioActual usuarioActual = new UsuarioActual();

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void tomaElUsuarioDelSubDelJwt() {
		Jwt jwt = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("42")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		assertThat(usuarioActual.id()).contains(42L);
	}

	@Test
	void tomaElMiembroVinculadoYLosRolesBaseDelJwt() {
		autenticar(Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("42")
			.claim(ClaimsJwt.MIEMBRO_ID, 25)
			.claim(ClaimsJwt.ROLES, List.of("LIDER", "MIEMBRO")));

		assertThat(usuarioActual.miembroId()).contains(25L);
		assertThat(usuarioActual.rolesBase()).containsExactlyInAnyOrder("LIDER", "MIEMBRO");
	}

	@Test
	void sinMiembroNiRolesDevuelveVacio() {
		autenticar(Jwt.withTokenValue("token").header("alg", "RS256").subject("42"));

		assertThat(usuarioActual.miembroId()).isEmpty();
		assertThat(usuarioActual.rolesBase()).isEmpty();
	}

	@Test
	void sinJwtNoHayUsuario() {
		assertThat(usuarioActual.id()).isEmpty();

		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("x", "y"));
		assertThat(usuarioActual.id()).isEmpty();
		assertThat(usuarioActual.rolesBase()).isEmpty();
	}

	private static void autenticar(Jwt.Builder jwt) {
		Jwt token = jwt.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
	}

}
