package com.iglesia.api.asistencia.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.asistencia.dto.CultoProgramadoResponse;
import com.iglesia.api.asistencia.dto.GuardarCultoRequest;
import com.iglesia.api.asistencia.entity.AsistenciaPrueba;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.repository.CultoProgramadoRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.grupos.model.DiaSemana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CultoProgramadoServiceTest {

	@Mock
	private CultoProgramadoRepository cultos;

	@Mock
	private UsuarioActual usuarioActual;

	private CultoProgramadoService servicio;

	@BeforeEach
	void configurar() {
		servicio = new CultoProgramadoService(cultos, new AsistenciaMapper(), usuarioActual);
	}

	@Test
	void listaDeLunesADomingoYPorHora() {
		given(cultos.findAll()).willReturn(List.of(
				AsistenciaPrueba.culto(1L, "Dominical tarde", DiaSemana.DOMINGO, LocalTime.of(18, 0)),
				AsistenciaPrueba.culto(2L, "Dominical", DiaSemana.DOMINGO, LocalTime.of(10, 0)),
				AsistenciaPrueba.culto(3L, "Oración", DiaSemana.MIERCOLES, LocalTime.of(19, 30))));

		assertThat(servicio.listar()).extracting(CultoProgramadoResponse::id).containsExactly(3L, 2L, 1L);
	}

	@Test
	void creaElCultoConElNombreNormalizado() {
		given(cultos.findAll()).willReturn(List.of());
		given(cultos.saveAndFlush(any())).willAnswer(invocacion -> invocacion.getArgument(0));

		CultoProgramadoResponse creado = servicio
			.crear(new GuardarCultoRequest("  Culto   dominical ", DiaSemana.DOMINGO, LocalTime.of(10, 0)));

		assertThat(creado.nombre()).isEqualTo("Culto dominical");
		assertThat(creado.activo()).isTrue();
	}

	@Test
	void unNombreRepetidoSinDistinguirMayusculasEsConflicto() {
		given(cultos.findAll())
			.willReturn(List.of(AsistenciaPrueba.culto(1L, "Culto de oración", DiaSemana.MIERCOLES, LocalTime.NOON)));

		assertThatThrownBy(
				() -> servicio.crear(new GuardarCultoRequest("CULTO DE ORACIÓN", DiaSemana.JUEVES, LocalTime.NOON)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(CultoProgramadoService.NOMBRE_REPETIDO);
	}

	@Test
	void editarConservandoSuPropioNombreNoEsConflicto() {
		var culto = AsistenciaPrueba.culto(1L, "Dominical", DiaSemana.DOMINGO, LocalTime.of(10, 0));
		given(cultos.findById(1L)).willReturn(Optional.of(culto));
		given(cultos.findAll()).willReturn(List.of(culto));

		CultoProgramadoResponse editado = servicio
			.actualizar(1L, new GuardarCultoRequest("Dominical", DiaSemana.DOMINGO, LocalTime.of(11, 0)));

		assertThat(editado.hora()).isEqualTo(LocalTime.of(11, 0));
	}

	@Test
	void desactivaYUnCultoInexistenteRespondeNoEncontrado() {
		var culto = AsistenciaPrueba.culto(1L, "Dominical", DiaSemana.DOMINGO, LocalTime.of(10, 0));
		given(cultos.findById(1L)).willReturn(Optional.of(culto));

		assertThat(servicio.cambiarEstado(1L, false).activo()).isFalse();
		assertThatThrownBy(() -> servicio.cambiarEstado(9L, true)).isInstanceOf(RecursoNoEncontradoException.class);
	}

}
