package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.dto.GuardarRondaRequest;
import com.iglesia.api.consolidacion.entity.ConsolidacionPrueba;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.RondaConsolidacionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RondaServiceTest {

	@Mock
	private RondaConsolidacionRepository rondas;

	@Mock
	private CasoConsolidacionRepository casos;

	@Mock
	private UsuarioActual usuarioActual;

	private RondaService servicio;

	@BeforeEach
	void configurar() {
		servicio = new RondaService(rondas, casos, new ConsolidacionMapper(), usuarioActual,
				Clock.fixed(ConsolidacionPrueba.AHORA, ZoneOffset.UTC));
	}

	@Test
	void laFechaDeFinNoPuedeSerAnteriorALaDeInicio() {
		var solicitud = new GuardarRondaRequest("Noviembre", null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

		assertThatThrownBy(() -> servicio.crear(solicitud)).isInstanceOf(ReglaNegocioException.class)
			.hasMessage(RondaService.FECHAS_INVALIDAS);
	}

	@Test
	void elNombreEsUnicoSinDistinguirMayusculasNiTildes() {
		given(rondas.findAll()).willReturn(List.of(ConsolidacionPrueba.ronda(1L, "Consolidación noviembre", EstadoRonda.PLANIFICADA)));

		assertThatThrownBy(() -> servicio
			.crear(new GuardarRondaRequest("CONSOLIDACIÓN  NOVIEMBRE", null, LocalDate.of(2026, 11, 2), null)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(RondaService.NOMBRE_REPETIDO);
	}

	@Test
	void noSeIniciaUnaRondaVacia() {
		given(rondas.findById(7L)).willReturn(Optional.of(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.PLANIFICADA)));
		given(casos.countByRondaId(7L)).willReturn(0L);

		assertThatThrownBy(() -> servicio.iniciar(7L)).isInstanceOf(ConflictoException.class)
			.hasMessage(RondaService.RONDA_VACIA);
	}

	@Test
	void noSeCierraUnaRondaPlanificada() {
		given(rondas.findById(7L)).willReturn(Optional.of(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.PLANIFICADA)));

		assertThatThrownBy(() -> servicio.cerrar(7L)).isInstanceOf(ConflictoException.class);
		then(casos).should(never()).cerrarAbiertosDeRonda(any(), any(), any());
	}

	@Test
	void alCerrarLosCasosAbiertosQuedanComoNoConcluidos() {
		RondaConsolidacion ronda = ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.EN_CURSO);
		given(rondas.findById(7L)).willReturn(Optional.of(ronda));
		given(usuarioActual.id()).willReturn(Optional.of(3L));
		given(casos.resumenDe(anyCollection(), any())).willReturn(List.of());

		servicio.cerrar(7L);

		then(casos).should().cerrarAbiertosDeRonda(eq(7L), eq(3L), eq(ConsolidacionPrueba.AHORA));
		assertThat(ronda.getEstado()).isEqualTo(EstadoRonda.CERRADA);
	}

	@Test
	void soloSeBorraUnaRondaPlanificada() {
		given(rondas.findById(7L)).willReturn(Optional.of(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.EN_CURSO)));

		assertThatThrownBy(() -> servicio.borrar(7L)).isInstanceOf(ConflictoException.class)
			.hasMessage(RondaService.SOLO_PLANIFICADA);
	}

	@Test
	void unaRondaCerradaNoSeEdita() {
		given(rondas.findById(7L)).willReturn(Optional.of(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.CERRADA)));

		assertThatThrownBy(() -> servicio.editable(7L)).isInstanceOf(ConflictoException.class)
			.hasMessage(RondaService.RONDA_CERRADA);
	}

}
