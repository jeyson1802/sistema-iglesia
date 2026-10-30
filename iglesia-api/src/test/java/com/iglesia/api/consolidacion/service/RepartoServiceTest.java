package com.iglesia.api.consolidacion.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.dto.AsignacionRequest;
import com.iglesia.api.consolidacion.dto.CargaConsolidadorResponse;
import com.iglesia.api.consolidacion.dto.PropuestaRepartoResponse;
import com.iglesia.api.consolidacion.dto.RepartirRequest;
import com.iglesia.api.consolidacion.entity.ConsolidacionPrueba;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.ConteoPorMiembro;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RepartoServiceTest {

	@Mock
	private RondaService rondas;

	@Mock
	private EquipoConsolidacionService equipo;

	@Mock
	private CasoConsolidacionRepository casos;

	@Mock
	private MiembrosParaConsolidacion miembros;

	@Mock
	private UsuarioActual usuarioActual;

	private RepartoService servicio;

	@BeforeEach
	void configurar() {
		servicio = new RepartoService(rondas, equipo, casos, miembros, new ConsolidacionMapper(), usuarioActual);
	}

	@Test
	void laPropuestaNoCuentaComoCargaLosCasosQueSeVuelvenARepartir() {
		given(rondas.editable(7L)).willReturn(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.PLANIFICADA));
		given(casos.findByRondaIdAndEstadoNotOrderById(7L, EstadoCaso.CERRADO))
			.willReturn(List.of(ConsolidacionPrueba.caso(1L, 7L, 40L, 10L, EstadoCaso.PENDIENTE),
					ConsolidacionPrueba.caso(2L, 7L, 41L, 10L, EstadoCaso.PENDIENTE)));
		given(equipo.paraRepartir(List.of(10L, 20L))).willReturn(List.of(10L, 20L));
		given(casos.abiertosPorConsolidador(List.of(10L, 20L))).willReturn(List.of(new ConteoPorMiembro(10L, 2)));
		given(miembros.porIds(anyCollection())).willReturn(Map.of(40L, persona(40L), 41L, persona(41L),
				10L, persona(10L), 20L, persona(20L)));

		PropuestaRepartoResponse propuesta = servicio.vistaPrevia(7L, new RepartirRequest(List.of(10L, 20L), false, null));

		assertThat(propuesta.cargas()).extracting(CargaConsolidadorResponse::cargaActual).containsExactly(0L, 0L);
		assertThat(propuesta.cargas()).extracting(CargaConsolidadorResponse::nuevos).containsExactly(1L, 1L);
	}

	@Test
	void lasAsignacionesExactasDebenSerDeCasosAbiertosDeLaRonda() {
		given(rondas.editable(7L)).willReturn(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.PLANIFICADA));
		given(casos.findByRondaIdAndEstadoNotOrderById(7L, EstadoCaso.CERRADO))
			.willReturn(List.of(ConsolidacionPrueba.caso(1L, 7L, 40L, null, EstadoCaso.PENDIENTE)));

		assertThatThrownBy(() -> servicio.repartir(7L,
				new RepartirRequest(null, false, List.of(new AsignacionRequest(99L, 10L)))))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(RepartoService.CASO_AJENO);
	}

	@Test
	void soloSeReasignanCasosAbiertos() {
		given(casos.findAllById(List.of(1L)))
			.willReturn(List.of(ConsolidacionPrueba.caso(1L, 7L, 40L, 10L, EstadoCaso.CERRADO)));
		given(equipo.personaActiva(20L)).willReturn(persona(20L));

		assertThatThrownBy(() -> servicio.reasignar(List.of(1L), 20L)).isInstanceOf(ReglaNegocioException.class)
			.hasMessage(RepartoService.CASO_CERRADO);
	}

	private static PersonaConsolidacion persona(Long id) {
		return new PersonaConsolidacion(id, "Persona " + id, null, null, null, null, true);
	}

}
