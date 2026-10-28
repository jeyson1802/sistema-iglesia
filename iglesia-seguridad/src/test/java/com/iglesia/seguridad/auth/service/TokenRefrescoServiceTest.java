package com.iglesia.seguridad.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.seguridad.auth.entity.TokenRefresco;
import com.iglesia.seguridad.auth.exception.TokenRefrescoInvalidoException;
import com.iglesia.seguridad.auth.repository.TokenRefrescoRepository;
import com.iglesia.seguridad.jwt.config.JwtProperties;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TokenRefrescoServiceTest {

	private static final Instant AHORA = Instant.parse("2026-01-01T10:00:00Z");

	private static final Duration VIGENCIA = Duration.ofDays(7);

	@Mock
	private TokenRefrescoRepository tokens;

	private TokenRefrescoService servicio;

	private Usuario usuario;

	@BeforeEach
	void configurar() {
		JwtProperties propiedades = new JwtProperties("e", List.of("a"), "a", Duration.ofMinutes(15), VIGENCIA, null,
				null);
		servicio = new TokenRefrescoService(tokens, propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));
		usuario = Usuario.nuevo("u@i.com", "h", 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);
		ReflectionTestUtils.setField(usuario, "id", 5L);
	}

	@Test
	void emiteUnValorAleatorioYGuardaSoloSuHash() {
		String valor = servicio.emitir(usuario);

		ArgumentCaptor<TokenRefresco> guardado = ArgumentCaptor.forClass(TokenRefresco.class);
		then(tokens).should().save(guardado.capture());
		String hash = (String) ReflectionTestUtils.getField(guardado.getValue(), "hashToken");
		assertThat(valor).hasSizeGreaterThanOrEqualTo(43);
		assertThat(hash).hasSize(64).isNotEqualTo(valor);
		assertThat(ReflectionTestUtils.getField(guardado.getValue(), "expiraEn")).isEqualTo(AHORA.plus(VIGENCIA));
		assertThat(servicio.emitir(usuario)).isNotEqualTo(valor);
	}

	@Test
	void consumirRevocaElTokenYDevuelveElUsuario() {
		TokenRefresco token = new TokenRefresco(usuario, "h", AHORA, AHORA.plus(VIGENCIA));
		given(tokens.findByHashToken(anyString())).willReturn(Optional.of(token));

		assertThat(servicio.consumir("valor")).isSameAs(usuario);
		assertThat(token.estaRevocado()).isTrue();
	}

	@Test
	void reutilizarUnTokenRevocadoRevocaTodasLasSesiones() {
		TokenRefresco token = new TokenRefresco(usuario, "h", AHORA, AHORA.plus(VIGENCIA));
		token.revocar(AHORA);
		given(tokens.findByHashToken(anyString())).willReturn(Optional.of(token));

		assertThatThrownBy(() -> servicio.consumir("valor")).isInstanceOf(TokenRefrescoInvalidoException.class);
		then(tokens).should().revocarVigentesDelUsuario(5L, AHORA);
	}

	@Test
	void rechazaTokensExpiradosInexistentesODeUsuariosInactivos() {
		given(tokens.findByHashToken(anyString())).willReturn(Optional.empty());
		assertThatThrownBy(() -> servicio.consumir("x")).isInstanceOf(TokenRefrescoInvalidoException.class);

		TokenRefresco expirado = new TokenRefresco(usuario, "h", AHORA.minus(VIGENCIA), AHORA);
		given(tokens.findByHashToken(anyString())).willReturn(Optional.of(expirado));
		assertThatThrownBy(() -> servicio.consumir("x")).isInstanceOf(TokenRefrescoInvalidoException.class);

		usuario.actualizar(RolesPrueba.bases(RolBase.ADMIN_IGLESIA), null, false);
		TokenRefresco deInactivo = new TokenRefresco(usuario, "h", AHORA, AHORA.plus(VIGENCIA));
		given(tokens.findByHashToken(anyString())).willReturn(Optional.of(deInactivo));
		assertThatThrownBy(() -> servicio.consumir("x")).isInstanceOf(TokenRefrescoInvalidoException.class);
	}

	@Test
	void revocarUnTokenInexistenteNoFalla() {
		given(tokens.findByHashToken(any())).willReturn(Optional.empty());

		servicio.revocar("no-existe");
	}

}
