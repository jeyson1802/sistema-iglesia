package com.iglesia.seguridad.auth;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.iglesia.seguridad.jwt.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;

class CookieRefrescoTest {

	private static final Duration DURACION_REFRESCO = Duration.ofDays(7);

	private final CookieRefresco cookieRefresco = new CookieRefresco(
			new CookieRefrescoProperties("iglesia_refresco", "/api/auth"),
			new JwtProperties("http://emisor", List.of("iglesia-seguridad"), "iglesia-seguridad",
					Duration.ofMinutes(15), DURACION_REFRESCO, null, null));

	@Test
	void emiteLaCookieConLosAtributosDeSeguridadYLaDuracionDelRefresco() {
		ResponseCookie cookie = cookieRefresco.emitir("valor-opaco");

		assertThat(cookie.getName()).isEqualTo("iglesia_refresco");
		assertThat(cookie.getValue()).isEqualTo("valor-opaco");
		assertThat(cookie.getPath()).isEqualTo("/api/auth");
		assertThat(cookie.getMaxAge()).isEqualTo(DURACION_REFRESCO);
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Strict");
	}

	@Test
	void borraLaCookieConValorVacioYMaxAgeCero() {
		ResponseCookie cookie = cookieRefresco.borrar();

		assertThat(cookie.getName()).isEqualTo("iglesia_refresco");
		assertThat(cookie.getValue()).isEmpty();
		assertThat(cookie.getPath()).isEqualTo("/api/auth");
		assertThat(cookie.getMaxAge()).isZero();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Strict");
	}

}
