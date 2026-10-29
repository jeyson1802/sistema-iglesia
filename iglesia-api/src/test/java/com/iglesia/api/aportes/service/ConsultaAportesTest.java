package com.iglesia.api.aportes.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.model.AlcanceAportes;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.common.error.exception.ReglaNegocioException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConsultaAportesTest {

	private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);

	@Mock
	private AmbitoAportes ambito;

	@InjectMocks
	private ConsultaAportes consulta;

	@Test
	void rechazaUnRangoInvertido() {
		assertThatThrownBy(() -> consulta.criterio(filtro(INICIO, INICIO.minusDays(1), null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ConsultaAportes.RANGO_INVERTIDO);
	}

	@Test
	void admiteHasta366DiasYRechazaMas() {
		given(ambito.actual()).willReturn(AlcanceAportes.completo());

		assertThat(consulta.criterio(filtro(INICIO, INICIO.plusDays(365), null))).isPresent();
		assertThatThrownBy(() -> consulta.criterio(filtro(INICIO, INICIO.plusDays(366), null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ConsultaAportes.RANGO_EXCESIVO);
	}

	@Test
	void aplicaElAmbitoAlFiltroDeMiembro() {
		given(ambito.actual()).willReturn(AlcanceAportes.soloDe(3L));

		assertThat(consulta.criterio(filtro(INICIO, INICIO, 9L)).orElseThrow().miembroId()).isEqualTo(3L);
	}

	@Test
	void sinAmbitoNoHayCriterio() {
		given(ambito.actual()).willReturn(AlcanceAportes.ninguno());

		assertThat(consulta.criterio(filtro(INICIO, INICIO, null))).isEmpty();
	}

	private static FiltroAportes filtro(LocalDate desde, LocalDate hasta, Long miembroId) {
		return new FiltroAportes(desde, hasta, null, MedioAporte.YAPE, miembroId, null);
	}

}
