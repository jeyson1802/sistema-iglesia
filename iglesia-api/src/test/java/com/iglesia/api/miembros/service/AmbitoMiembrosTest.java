package com.iglesia.api.miembros.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.model.AmbitoLectura;
import com.iglesia.api.miembros.repository.MiembroRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AmbitoMiembrosTest {

	@Mock
	private UsuarioActual usuarioActual;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private MiembroRepository miembros;

	private AmbitoMiembros ambito;

	@BeforeEach
	void configurar() {
		ambito = new AmbitoMiembros(usuarioActual, iglesiaActual, miembros);
	}

	@Test
	void losAdministradoresVenTodaLaIglesia() {
		given(usuarioActual.rolesBase()).willReturn(Set.of("ADMIN_IGLESIA"));
		assertThat(ambito.actual()).isEqualTo(AmbitoLectura.completo());

		given(usuarioActual.rolesBase()).willReturn(Set.of("ADMIN_SISTEMA"));
		assertThat(ambito.actual()).isEqualTo(AmbitoLectura.completo());
	}

	@Test
	void elLiderVeASiMismoSuRedYSuCadena() {
		given(usuarioActual.rolesBase()).willReturn(Set.of("LIDER"));
		given(usuarioActual.miembroId()).willReturn(Optional.of(5L));
		given(iglesiaActual.id()).willReturn(Optional.of(1L));
		given(miembros.existsById(5L)).willReturn(true);
		given(miembros.idsDeLaRed(1L, 5L)).willReturn(List.of(7L, 8L));
		given(miembros.idsDeLaCadena(1L, 5L)).willReturn(List.of(3L, 1L));

		AmbitoLectura visible = ambito.actual();

		assertThat(visible.ids()).containsExactlyInAnyOrder(5L, 7L, 8L, 3L, 1L);
		assertThat(visible.incluye(9L)).isFalse();
	}

	@Test
	void unLiderSinMiembroOConUnMiembroDeOtraIglesiaNoVeANadie() {
		given(usuarioActual.rolesBase()).willReturn(Set.of("LIDER"));
		given(usuarioActual.miembroId()).willReturn(Optional.empty());
		assertThat(ambito.actual()).isEqualTo(AmbitoLectura.ninguno());

		given(usuarioActual.miembroId()).willReturn(Optional.of(5L));
		given(iglesiaActual.id()).willReturn(Optional.of(1L));
		given(miembros.existsById(5L)).willReturn(false);
		assertThat(ambito.actual()).isEqualTo(AmbitoLectura.ninguno());
	}

	@Test
	void otrosRolesNoVenANadie() {
		given(usuarioActual.rolesBase()).willReturn(Set.of("MIEMBRO"));

		assertThat(ambito.actual().incluye(1L)).isFalse();
	}

}
