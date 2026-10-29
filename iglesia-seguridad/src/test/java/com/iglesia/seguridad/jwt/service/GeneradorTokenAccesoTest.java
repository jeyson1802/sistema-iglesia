package com.iglesia.seguridad.jwt.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.seguridad.jwt.config.JwtProperties;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;
import com.iglesia.seguridad.jwt.model.TokenAcceso;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.entity.Usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class GeneradorTokenAccesoTest {

	private static final Instant AHORA = Instant.now();

	private static final Duration DURACION = Duration.ofMinutes(15);

	private RSAKey clave;

	private GeneradorTokenAcceso generador;

	private RolService servicioRoles;

	@BeforeEach
	void configurar() throws Exception {
		clave = new RSAKeyGenerator(2048).keyID("clave-prueba").generate();
		JwtProperties propiedades = new JwtProperties("http://emisor", List.of("iglesia-api", "iglesia-seguridad"),
				"iglesia-seguridad", DURACION, Duration.ofDays(7), null, null);
		servicioRoles = mock(RolService.class);
		generador = new GeneradorTokenAcceso(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave))),
				propiedades, clave, Clock.fixed(AHORA, ZoneOffset.UTC), servicioRoles);
	}

	@Test
	void incluyeRolesIglesiaYMiembroFirmadoConRs256() throws Exception {
		Usuario usuario = Usuario.nuevo("l@i.com", "h", 3L, 9L, RolesPrueba.bases(RolBase.MIEMBRO, RolBase.LIDER), false);
		ReflectionTestUtils.setField(usuario, "id", 42L);

		TokenAcceso token = generador.generar(usuario, null);
		Jwt jwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build().decode(token.valor());

		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", "clave-prueba");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("LIDER", "MIEMBRO");
		assertThat(jwt.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(3L);
		assertThat(jwt.<Long>getClaim(ClaimsJwt.MIEMBRO_ID)).isEqualTo(9L);
		assertThat(jwt.getAudience()).containsExactly("iglesia-api", "iglesia-seguridad");
		assertThat(token.expiraEn()).isEqualTo(AHORA.plus(DURACION));
		assertThat(jwt.getClaims()).doesNotContainKey("correo");
	}

	@Test
	void llevaLosRolesBaseSinRepetirYLaUnionOrdenadaDeLosPermisosDeLosRolesActivos() throws Exception {
		Rol secretaria = RolesPrueba.personalizado(10L, 3L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver",
				"miembros.crear");
		Rol consejo = RolesPrueba.personalizado(11L, 3L, "Consejo", RolBase.ADMIN_IGLESIA, "miembros.ver",
				"usuarios.ver");
		Rol tesoreria = RolesPrueba.desactivado(
				RolesPrueba.personalizado(12L, 3L, "Tesorería", RolBase.LIDER, "roles.ver"));
		Usuario usuario = Usuario.nuevo("s@i.com", "h", 3L, 9L, Set.of(secretaria, consejo, tesoreria), false);
		ReflectionTestUtils.setField(usuario, "id", 43L);

		Jwt jwt = decodificar(generador.generar(usuario, null));

		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_IGLESIA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("miembros.crear", "miembros.ver",
				"usuarios.ver");
	}

	@Test
	void unRolBaseSinPermisosEmiteLaListaDePermisosVacia() throws Exception {
		Usuario miembro = Usuario.nuevo("m@i.com", "h", 3L, 9L, RolesPrueba.bases(RolBase.MIEMBRO), false);
		ReflectionTestUtils.setField(miembro, "id", 44L);

		assertThat(decodificar(generador.generar(miembro, null)).getClaimAsStringList(ClaimsJwt.PERMISOS)).isEmpty();
	}

	@Test
	void elAdministradorDelSistemaNoLlevaIglesia() throws Exception {
		Usuario admin = Usuario.nuevo("a@i.com", "h", null, null, RolesPrueba.bases(RolBase.ADMIN_SISTEMA), false);
		ReflectionTestUtils.setField(admin, "id", 1L);

		Jwt jwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey())
			.build()
			.decode(generador.generar(admin, null).valor());

		assertThat(jwt.hasClaim(ClaimsJwt.IGLESIA_ID)).isFalse();
		assertThat(jwt.hasClaim(ClaimsJwt.MIEMBRO_ID)).isFalse();
		assertThat(jwt.hasClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isFalse();
		then(servicioRoles).shouldHaveNoInteractions();
	}

	@Test
	void dentroDeUnaIglesiaLlevaEsaIglesiaYSumaLosPermisosDeUnAdministradorDeIglesia() throws Exception {
		Usuario admin = Usuario.nuevo("a@i.com", "h", null, null,
				Set.of(RolesPrueba.base(RolBase.ADMIN_SISTEMA, "iglesias.ver", "iglesias.administrar", "usuarios.ver")),
				false);
		ReflectionTestUtils.setField(admin, "id", 1L);
		given(servicioRoles.permisosDeRolBase(RolBase.ADMIN_IGLESIA))
			.willReturn(Set.of("usuarios.ver", "roles.administrar"));

		Jwt jwt = decodificar(generador.generar(admin, 7L));

		assertThat(jwt.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(7L);
		assertThat(jwt.<Object>getClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isEqualTo(true);
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_SISTEMA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("iglesias.administrar",
				"iglesias.ver", "roles.administrar", "usuarios.ver");
	}

	@Test
	void emiteSiempreSiLaContrasenaEsTemporalComoBooleano() throws Exception {
		Usuario temporal = Usuario.nuevo("t@i.com", "h", 3L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), true);
		Usuario definitiva = Usuario.nuevo("d@i.com", "h", 3L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);
		ReflectionTestUtils.setField(temporal, "id", 45L);
		ReflectionTestUtils.setField(definitiva, "id", 46L);

		assertThat(decodificar(generador.generar(temporal, null)).<Object>getClaim(ClaimsJwt.CAMBIO_CONTRASENA))
			.isEqualTo(true);
		assertThat(decodificar(generador.generar(definitiva, null)).<Object>getClaim(ClaimsJwt.CAMBIO_CONTRASENA))
			.isEqualTo(false);
	}

	private Jwt decodificar(TokenAcceso token) throws Exception {
		return NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build().decode(token.valor());
	}

}
