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
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorTokenAccesoTest {

	private static final Instant AHORA = Instant.now();

	private static final Duration DURACION = Duration.ofMinutes(15);

	private RSAKey clave;

	private GeneradorTokenAcceso generador;

	@BeforeEach
	void configurar() throws Exception {
		clave = new RSAKeyGenerator(2048).keyID("clave-prueba").generate();
		JwtProperties propiedades = new JwtProperties("http://emisor", List.of("iglesia-api", "iglesia-seguridad"),
				"iglesia-seguridad", DURACION, Duration.ofDays(7), null, null);
		generador = new GeneradorTokenAcceso(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave))),
				propiedades, clave, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void incluyeRolesIglesiaYMiembroFirmadoConRs256() throws Exception {
		Usuario usuario = Usuario.nuevo("l@i.com", "h", 3L, 9L, RolesPrueba.bases(RolBase.MIEMBRO, RolBase.LIDER));
		ReflectionTestUtils.setField(usuario, "id", 42L);

		TokenAcceso token = generador.generar(usuario);
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
	void elAdministradorDelSistemaNoLlevaIglesia() throws Exception {
		Usuario admin = Usuario.nuevo("a@i.com", "h", null, null, RolesPrueba.bases(RolBase.ADMIN_SISTEMA));
		ReflectionTestUtils.setField(admin, "id", 1L);

		Jwt jwt = NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey())
			.build()
			.decode(generador.generar(admin).valor());

		assertThat(jwt.hasClaim(ClaimsJwt.IGLESIA_ID)).isFalse();
		assertThat(jwt.hasClaim(ClaimsJwt.MIEMBRO_ID)).isFalse();
	}

}
