package com.iglesia.api.asistencia.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.AlcanceSeguimiento;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.service.AgendaGrupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AmbitoAsistenciaTest {

	@Mock
	private UsuarioActual usuarioActual;

	@Mock
	private AgendaGrupos grupos;

	private AmbitoAsistencia ambito;

	@BeforeEach
	void configurar() {
		ambito = new AmbitoAsistencia(usuarioActual, grupos);
	}

	@Test
	void elUjierMarcaEnTodasPeroNoConsultaElHistorial() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_REGISTRAR)).willReturn(true);

		assertThat(ambito.paraMarcar()).isEqualTo(AlcanceAsistencia.completo());
		assertThat(ambito.paraVerReunion()).isEqualTo(AlcanceAsistencia.completo());
		assertThat(ambito.paraConsultar().vacio()).isTrue();
	}

	@Test
	void elLiderSoloActuaEnLosGruposQueLidera() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)).willReturn(true);
		given(usuarioActual.miembroId()).willReturn(Optional.of(12L));
		given(grupos.lideradosPor(12L)).willReturn(Set.of(8L));

		assertThat(ambito.paraMarcar()).isEqualTo(AlcanceAsistencia.deGrupos(Set.of(8L)));
		assertThat(ambito.paraConsultar()).isEqualTo(AlcanceAsistencia.deGrupos(Set.of(8L)));
	}

	@Test
	void sinMiembroVinculadoOSinPermisoNoActuaEnNinguna() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)).willReturn(true);
		given(usuarioActual.miembroId()).willReturn(Optional.empty());
		assertThat(ambito.paraMarcar().vacio()).isTrue();

		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)).willReturn(false);
		assertThat(ambito.paraConsultar().vacio()).isTrue();
	}

	@Test
	void quienVeElHistorialConsultaTodas() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(true);

		assertThat(ambito.paraConsultar()).isEqualTo(AlcanceAsistencia.completo());
	}

	@Test
	void elSeguimientoEsDeTodaLaIglesiaConVerYDeSuRedParaElLider() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(true);
		assertThat(ambito.paraSeguimiento()).isEqualTo(AlcanceSeguimiento.todaLaIglesia());

		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(false);
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)).willReturn(true);
		given(usuarioActual.miembroId()).willReturn(Optional.of(7L));
		assertThat(ambito.paraSeguimiento()).isEqualTo(AlcanceSeguimiento.redDe(7L));

		given(usuarioActual.miembroId()).willReturn(Optional.empty());
		assertThat(ambito.paraSeguimiento().vacio()).isTrue();
	}

}
