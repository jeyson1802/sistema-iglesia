package com.iglesia.api.aportes.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.aportes.model.AlcanceAportes;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.RolesBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AmbitoAportesTest {

	@Mock
	private UsuarioActual usuarioActual;

	@InjectMocks
	private AmbitoAportes ambito;

	@Test
	void losAdministradoresVenTodaLaIglesia() {
		given(usuarioActual.rolesBase()).willReturn(Set.of(RolesBase.ADMIN_IGLESIA));
		assertThat(ambito.actual()).isEqualTo(AlcanceAportes.completo());

		given(usuarioActual.rolesBase()).willReturn(Set.of(RolesBase.ADMIN_SISTEMA));
		assertThat(ambito.actual()).isEqualTo(AlcanceAportes.completo());
	}

	@Test
	void elLiderSoloVeLosSuyos() {
		given(usuarioActual.rolesBase()).willReturn(Set.of(RolesBase.LIDER));
		given(usuarioActual.miembroId()).willReturn(Optional.of(4L));

		assertThat(ambito.actual()).isEqualTo(AlcanceAportes.soloDe(4L));
	}

	@Test
	void sinMiembroVinculadoNoVeNada() {
		given(usuarioActual.rolesBase()).willReturn(Set.of());
		given(usuarioActual.miembroId()).willReturn(Optional.empty());

		assertThat(ambito.actual().vacio()).isTrue();
	}

}
