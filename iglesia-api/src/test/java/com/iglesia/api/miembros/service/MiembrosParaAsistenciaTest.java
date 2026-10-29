package com.iglesia.api.miembros.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.component.GeneradorCodigoAsistencia;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.mapper.MiembroMapper;
import com.iglesia.api.miembros.model.AmbitoLectura;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.model.MiembrosPrueba;
import com.iglesia.api.miembros.repository.MiembroRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class MiembrosParaAsistenciaTest {

	private static final CodigoAsistencia PRIMERO = new CodigoAsistencia("7K2QM9XDR4TB");

	private static final CodigoAsistencia SEGUNDO = new CodigoAsistencia("Z9Y8X7W6V5T4");

	@Mock
	private MiembroRepository miembros;

	@Mock
	private GeneradorCodigoAsistencia generador;

	@Mock
	private AmbitoMiembros ambito;

	@Mock
	private UsuarioActual usuarioActual;

	private MiembrosParaAsistencia servicio;

	@BeforeEach
	void configurar() {
		servicio = new MiembrosParaAsistencia(miembros, mock(MiembroMapper.class), generador, ambito, usuarioActual);
	}

	@Test
	void generaElCodigoDeUnMiembroAnteriorSinRegistrarloEnLosLogs(CapturedOutput salida) {
		Miembro ana = MiembrosPrueba.miembro(12L, "Ana", null);
		given(miembros.findById(12L)).willReturn(Optional.of(ana));
		given(ambito.actual()).willReturn(AmbitoLectura.completo());
		given(generador.generar()).willReturn(PRIMERO);

		assertThat(servicio.codigoDe(12L)).isEqualTo(PRIMERO);
		assertThat(ana.getCodigoAsistencia()).isEqualTo(PRIMERO.valor());
		assertThat(salida.getOut()).contains("Código de asistencia generado para miembroId=12")
			.doesNotContain(PRIMERO.valor())
			.doesNotContain("Ana");
	}

	@Test
	void conservaElCodigoQueYaTiene() {
		Miembro ana = MiembrosPrueba.miembro(12L, "Ana", null);
		ana.asignarCodigoAsistencia(PRIMERO);
		given(miembros.findById(12L)).willReturn(Optional.of(ana));
		given(ambito.actual()).willReturn(AmbitoLectura.completo());

		assertThat(servicio.codigoDe(12L)).isEqualTo(PRIMERO);
	}

	@Test
	void regenerarReemplazaElCodigoYReintentaSiChoca() {
		Miembro ana = MiembrosPrueba.miembro(12L, "Ana", null);
		ana.asignarCodigoAsistencia(PRIMERO);
		given(miembros.findById(12L)).willReturn(Optional.of(ana));
		given(ambito.actual()).willReturn(AmbitoLectura.completo());
		given(generador.generar()).willReturn(PRIMERO, SEGUNDO);
		given(miembros.existsByCodigoAsistencia(PRIMERO.valor())).willReturn(true);

		assertThat(servicio.regenerar(12L)).isEqualTo(SEGUNDO);
		assertThat(ana.getCodigoAsistencia()).isEqualTo(SEGUNDO.valor());
	}

	@Test
	void unMiembroFueraDelAmbitoRespondeNoEncontrado() {
		given(miembros.findById(12L)).willReturn(Optional.of(MiembrosPrueba.miembro(12L, "Ana", null)));
		given(ambito.actual()).willReturn(AmbitoLectura.soloDe(Set.of(30L)));

		assertThatThrownBy(() -> servicio.codigoDe(12L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio.regenerar(12L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

}
