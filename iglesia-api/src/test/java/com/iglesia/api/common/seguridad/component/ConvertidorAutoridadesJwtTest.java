package com.iglesia.api.common.seguridad.component;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertidorAutoridadesJwtTest {

	private final ConvertidorAutoridadesJwt convertidor = new ConvertidorAutoridadesJwt();

	@Test
	void losRolesLlevanPrefijoYLosPermisosNo() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.ROLES, List.of("LIDER"))
			.claim(ClaimsJwt.PERMISOS, List.of("miembros.ver"))
			.build();

		// Spring Security agrega además su propia autoridad de factor (FACTOR_BEARER).
		assertThat(autoridades(jwt)).contains("ROLE_LIDER", "miembros.ver")
			.doesNotContain("LIDER", "ROLE_miembros.ver");
	}

	@Test
	void unaSecretariaLlevaSuRolBaseYSoloSusPermisos() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.ROLES, List.of("ADMIN_IGLESIA"))
			.claim(ClaimsJwt.PERMISOS, List.of("miembros.crear", "miembros.ver"))
			.build();

		assertThat(autoridades(jwt)).filteredOn(autoridad -> !autoridad.startsWith("FACTOR_"))
			.containsExactlyInAnyOrder("ROLE_ADMIN_IGLESIA", "miembros.crear", "miembros.ver");
	}

	@Test
	void sinPermisosSoloQuedanLosRoles() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.ROLES, List.of("MIEMBRO")).build();

		assertThat(autoridades(jwt)).filteredOn(autoridad -> !autoridad.startsWith("FACTOR_"))
			.containsExactly("ROLE_MIEMBRO");
	}

	@Test
	void elNombreDeLaAutenticacionEsElSubject() {
		Jwt jwt = jwtBase().claim(ClaimsJwt.ROLES, List.of("MIEMBRO")).build();

		assertThat(convertidor.convert(jwt).getName()).isEqualTo("42");
	}

	private List<String> autoridades(Jwt jwt) {
		return convertidor.convert(jwt).getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
	}

	private static Jwt.Builder jwtBase() {
		return Jwt.withTokenValue("token").header("alg", "RS256").subject("42");
	}

}
