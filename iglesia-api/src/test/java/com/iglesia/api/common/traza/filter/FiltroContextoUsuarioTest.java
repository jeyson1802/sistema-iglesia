package com.iglesia.api.common.traza.filter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroContextoUsuarioTest {

	private final FiltroContextoUsuario filtro = new FiltroContextoUsuario();

	/** MDC visto por el resto de la cadena mientras se atiende la petición. */
	private final Map<String, String> mdcDuranteLaPeticion = new HashMap<>();

	private final FilterChain cadena = (peticion, respuesta) -> {
		Map<String, String> copia = MDC.getCopyOfContextMap();
		if (copia != null) {
			mdcDuranteLaPeticion.putAll(copia);
		}
	};

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
		MDC.clear();
	}

	@Test
	void conJwtAgregaUsuarioEIglesiaYLosQuitaAlTerminar() throws Exception {
		autenticarConJwt(Map.of(ClaimsJwt.IGLESIA_ID, 7L));

		filtrar();

		assertThat(mdcDuranteLaPeticion).containsEntry(TrazaConstantes.MDC_USUARIO_ID, "15")
			.containsEntry(TrazaConstantes.MDC_IGLESIA_ID, "7");
		assertThat(MDC.get(TrazaConstantes.MDC_USUARIO_ID)).isNull();
		assertThat(MDC.get(TrazaConstantes.MDC_IGLESIA_ID)).isNull();
	}

	@Test
	void sinIglesiaEnElTokenSoloAgregaElUsuario() throws Exception {
		autenticarConJwt(Map.of());

		filtrar();

		assertThat(mdcDuranteLaPeticion).containsEntry(TrazaConstantes.MDC_USUARIO_ID, "15")
			.doesNotContainKey(TrazaConstantes.MDC_IGLESIA_ID);
	}

	@Test
	void sinJwtNoAgregaNada() throws Exception {
		SecurityContextHolder.getContext()
			.setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("correo", "clave"));

		filtrar();

		assertThat(mdcDuranteLaPeticion).doesNotContainKeys(TrazaConstantes.MDC_USUARIO_ID,
				TrazaConstantes.MDC_IGLESIA_ID);
	}

	private void filtrar() throws Exception {
		filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), cadena);
	}

	private static void autenticarConJwt(Map<String, Object> claims) {
		Jwt jwt = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("15")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.claims(existentes -> existentes.putAll(claims))
			.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

}
