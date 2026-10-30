package com.iglesia.api.consolidacion.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ConsolidacionPrueba;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AmbitoConsolidacionTest {

	private static final Long YO = 25L;

	@Mock
	private UsuarioActual usuarioActual;

	@Mock
	private MiembrosParaConsolidacion miembros;

	@Mock
	private CasoConsolidacionRepository casos;

	private AmbitoConsolidacion ambito;

	@BeforeEach
	void configurar() {
		ambito = new AmbitoConsolidacion(usuarioActual, miembros, casos);
		given(usuarioActual.tienePermiso(anyString())).willReturn(false);
		given(usuarioActual.miembroId()).willReturn(Optional.of(YO));
		given(miembros.red(YO)).willReturn(Set.of(70L));
	}

	@Test
	void elCoordinadorVeYTrabajaCualquierCasoSinOtrosPermisos() {
		con(Permisos.CONSOLIDACION_ADMINISTRAR);
		CasoConsolidacion ajeno = ConsolidacionPrueba.caso(1L, 7L, 40L, 99L, EstadoCaso.PENDIENTE);

		assertThat(ambito.puedeVer(ajeno)).isTrue();
		assertThat(ambito.puedeActuar(ajeno)).isTrue();
		assertThat(ambito.puedeVerPersona(40L)).isTrue();
		assertThat(ambito.puedeIniciarSeguimiento(40L)).isTrue();
		assertThat(ambito.consolidador()).contains(YO);
	}

	@Test
	void conVerSeVeTodoPeroNoSeTrabaja() {
		con(Permisos.CONSOLIDACION_VER);
		CasoConsolidacion ajeno = ConsolidacionPrueba.caso(1L, 7L, 40L, 99L, EstadoCaso.PENDIENTE);

		assertThat(ambito.puedeVer(ajeno)).isTrue();
		assertThat(ambito.puedeActuar(ajeno)).isFalse();
		assertThat(ambito.puedeIniciarSeguimiento(40L)).isFalse();
		assertThat(ambito.consolidador()).isEmpty();
	}

	@Test
	void elConsolidadorTrabajaSusCasosYNoLosAjenos() {
		con(Permisos.CONSOLIDACION_CONTACTAR);
		CasoConsolidacion propio = ConsolidacionPrueba.caso(1L, 7L, 40L, YO, EstadoCaso.PENDIENTE);
		CasoConsolidacion ajeno = ConsolidacionPrueba.caso(2L, 7L, 41L, 99L, EstadoCaso.PENDIENTE);

		assertThat(ambito.puedeActuar(propio)).isTrue();
		assertThat(ambito.puedeVer(ajeno)).isFalse();
		assertThat(ambito.puedeActuar(ajeno)).isFalse();
	}

	@Test
	void elConsolidadorVeElHistorialDeSusAsignadosYDeSuRed() {
		con(Permisos.CONSOLIDACION_CONTACTAR);
		given(casos.existsByConsolidadorIdAndMiembroIdAndEstadoNot(YO, 40L, EstadoCaso.CERRADO)).willReturn(true);

		assertThat(ambito.puedeVerPersona(40L)).isTrue();
		assertThat(ambito.puedeVerPersona(70L)).isTrue();
		assertThat(ambito.puedeVerPersona(80L)).isFalse();
	}

	@Test
	void elConsolidadorSoloIniciaSeguimientosASuRed() {
		con(Permisos.CONSOLIDACION_CONTACTAR);

		assertThat(ambito.puedeIniciarSeguimiento(70L)).isTrue();
		assertThat(ambito.puedeIniciarSeguimiento(80L)).isFalse();
	}

	private void con(String permiso) {
		given(usuarioActual.tienePermiso(permiso)).willReturn(true);
	}

}
