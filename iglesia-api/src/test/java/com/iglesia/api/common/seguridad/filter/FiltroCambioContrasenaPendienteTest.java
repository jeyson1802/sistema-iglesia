package com.iglesia.api.common.seguridad.filter;

import java.time.Instant;
import java.util.Map;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.exception.CambioContrasenaPendienteException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FiltroCambioContrasenaPendienteTest {

	@Mock
	private AccessDeniedHandler accesoDenegado;

	@Mock
	private FilterChain cadena;

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void conCambioPendienteBloqueaCualquierRuta() throws Exception {
		autenticarConJwt(Map.of(ClaimsJwt.CAMBIO_CONTRASENA, true));
		MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/miembros");
		MockHttpServletResponse respuesta = new MockHttpServletResponse();

		filtro().doFilter(peticion, respuesta, cadena);

		then(accesoDenegado).should()
			.handle(eq(peticion), eq(respuesta), any(CambioContrasenaPendienteException.class));
		then(cadena).should(never()).doFilter(any(), any());
	}

	@Test
	void sinCambioPendienteNoBloquea() throws Exception {
		autenticarConJwt(Map.of(ClaimsJwt.CAMBIO_CONTRASENA, false));

		filtrar("GET", "/api/miembros");

		then(accesoDenegado).should(never()).handle(any(), any(), any());
		then(cadena).should().doFilter(any(), any());
	}

	@Test
	void unTokenSinElClaimSeTrataComoSinCambioPendiente() throws Exception {
		autenticarConJwt(Map.of());

		filtrar("GET", "/api/miembros");

		then(accesoDenegado).should(never()).handle(any(), any(), any());
		then(cadena).should().doFilter(any(), any());
	}

	@Test
	void sinJwtNoBloquea() throws Exception {
		filtrar("GET", "/actuator/health");

		then(accesoDenegado).should(never()).handle(any(), any(), any());
		then(cadena).should().doFilter(any(), any());
	}

	private FiltroCambioContrasenaPendiente filtro() {
		return new FiltroCambioContrasenaPendiente(accesoDenegado);
	}

	private void filtrar(String metodo, String ruta) throws Exception {
		filtro().doFilter(new MockHttpServletRequest(metodo, ruta), new MockHttpServletResponse(), cadena);
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
