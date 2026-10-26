package com.iglesia.api.common.multiiglesia;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class IglesiaActualTest {

	private final IglesiaActual iglesiaActual = new IglesiaActual();

	private final ResolutorIglesiaHibernate resolutor = new ResolutorIglesiaHibernate(iglesiaActual);

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void tomaLaIglesiaDelClaimDelJwt() {
		ContextoJwt.autenticarConIglesia(7L);

		assertThat(iglesiaActual.id()).contains(7L);
		assertThat(resolutor.resolveCurrentTenantIdentifier()).isEqualTo(7L);
	}

	@Test
	void sinAutenticacionNoHayIglesiaYHibernateNoVeNingunaFila() {
		assertThat(iglesiaActual.id()).isEmpty();
		assertThat(resolutor.resolveCurrentTenantIdentifier()).isEqualTo(ResolutorIglesiaHibernate.SIN_IGLESIA);
	}

	@Test
	void unTokenSinIglesiaNoTieneIglesia() {
		ContextoJwt.autenticarConIglesia(null);

		assertThat(iglesiaActual.id()).isEmpty();
	}

	@Test
	void ignoraAutenticacionesQueNoSonJwt() {
		SecurityContextHolder.getContext()
			.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("x", null, List.of()));

		assertThat(iglesiaActual.id()).isEmpty();
	}

}
