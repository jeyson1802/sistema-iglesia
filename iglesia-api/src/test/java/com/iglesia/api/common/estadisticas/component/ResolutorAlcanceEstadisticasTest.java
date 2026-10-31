package com.iglesia.api.common.estadisticas.component;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.iglesia.api.common.estadisticas.model.AlcanceEstadisticas;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ResolutorAlcanceEstadisticasTest {

	private final UsuarioActual usuario = mock(UsuarioActual.class);

	private final IglesiaActual iglesia = mock(IglesiaActual.class);

	private final ResolutorAlcanceEstadisticas resolutor = new ResolutorAlcanceEstadisticas(usuario, iglesia);

	@Test
	void elAdministradorVeTodaLaIglesia() {
		dentroDeIglesia(Set.of("ADMIN_IGLESIA"), null);
		assertThat(resolutor.actual()).isEqualTo(AlcanceEstadisticas.iglesia());
	}

	@Test
	void elAdministradorDelSistemaDentroDeUnaIglesiaVeTodaLaIglesia() {
		dentroDeIglesia(Set.of("ADMIN_SISTEMA"), null);
		assertThat(resolutor.actual().esRed()).isFalse();
	}

	@Test
	void elLiderVeSuRedYNoTieneIglesiaCompleta() {
		dentroDeIglesia(Set.of("LIDER"), 12L);
		assertThat(resolutor.actual()).isEqualTo(AlcanceEstadisticas.redDe(12L));
		assertThatThrownBy(resolutor::exigirIglesiaCompleta).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void unMiembroUnLiderSinVinculoOLaVistaGlobalNoTienenAlcance() {
		dentroDeIglesia(Set.of("MIEMBRO"), 12L);
		assertThatThrownBy(resolutor::actual).isInstanceOf(AccessDeniedException.class);
		dentroDeIglesia(Set.of("LIDER"), null);
		assertThatThrownBy(resolutor::actual).isInstanceOf(AccessDeniedException.class);
		given(iglesia.id()).willReturn(Optional.empty());
		assertThatThrownBy(resolutor::actual).isInstanceOf(AccessDeniedException.class);
	}

	private void dentroDeIglesia(Set<String> roles, Long miembroId) {
		given(iglesia.id()).willReturn(Optional.of(3L));
		given(usuario.rolesBase()).willReturn(roles);
		given(usuario.miembroId()).willReturn(Optional.ofNullable(miembroId));
	}

}
