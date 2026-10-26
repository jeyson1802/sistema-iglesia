package com.iglesia.seguridad.common.seguridad.model;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.iglesia.seguridad.jwt.constant.ClaimsJwt;
import com.iglesia.seguridad.rol.model.RolBase;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioAutenticadoTest {

	@Test
	void tomaRolesBaseYPermisosDelJwt() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.IGLESIA_ID, 3L)
			.claim(ClaimsJwt.ROLES, List.of("ADMIN_IGLESIA"))
			.claim(ClaimsJwt.PERMISOS, List.of("miembros.crear", "miembros.ver"))
			.build();

		UsuarioAutenticado usuario = UsuarioAutenticado.desde(jwt);

		assertThat(usuario.usuarioId()).isEqualTo(42L);
		assertThat(usuario.iglesiaId()).isEqualTo(3L);
		assertThat(usuario.roles()).containsExactly(RolBase.ADMIN_IGLESIA);
		assertThat(usuario.permisos()).containsExactlyInAnyOrder("miembros.crear", "miembros.ver");
	}

	@Test
	void sinClaimDePermisosNoTienePermisos() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.ROLES, List.of("MIEMBRO")).build();

		assertThat(UsuarioAutenticado.desde(jwt).permisos()).isEmpty();
	}

	private static Jwt.Builder jwtBase() {
		return Jwt.withTokenValue("token").header("alg", "RS256").subject("42");
	}

}
