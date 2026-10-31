package com.iglesia.seguridad.common.periodo.component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.seguridad.common.periodo.model.Granularidad;
import com.iglesia.seguridad.common.periodo.model.PeriodoEstadistico;
import com.iglesia.seguridad.common.periodo.model.RangoPeriodo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraPeriodoTest {

	/** Viernes 30 de octubre de 2026 al mediodía. */
	private static final LocalDate HOY = LocalDate.of(2026, 10, 30);

	private final CalculadoraPeriodo calculadora = new CalculadoraPeriodo(Clock.fixed(
			HOY.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

	@ParameterizedTest
	@CsvSource({
			"HOY, 2026-10-30, 2026-10-30, 2026-10-29, 2026-10-29, DIA, 1",
			"SEMANA, 2026-10-26, 2026-10-30, 2026-10-21, 2026-10-25, DIA, 5",
			"MES, 2026-10-01, 2026-10-30, 2026-09-01, 2026-09-30, SEMANA, 5",
			"MES_ANTERIOR, 2026-09-01, 2026-09-30, 2026-08-02, 2026-08-31, SEMANA, 5",
			"ANIO, 2026-01-01, 2026-10-30, 2025-03-04, 2025-12-31, MES, 10" })
	void calculaElPeriodoYElAnteriorDeLaMismaDuracion(PeriodoEstadistico periodo, LocalDate desde, LocalDate hasta,
			LocalDate anteriorDesde, LocalDate anteriorHasta, Granularidad granularidad, int puntos) {
		RangoPeriodo rango = calculadora.rango(ConsultaPeriodo.de(periodo));

		assertThat(rango.desde()).isEqualTo(desde);
		assertThat(rango.hasta()).isEqualTo(hasta);
		assertThat(rango.anteriorDesde()).isEqualTo(anteriorDesde);
		assertThat(rango.anteriorHasta()).isEqualTo(anteriorHasta);
		assertThat(rango.granularidad()).isEqualTo(granularidad);
		assertThat(rango.inicios()).hasSize(puntos);
	}

	@Test
	void sinPeriodoUsaElMes() {
		assertThat(calculadora.rango(new ConsultaPeriodo(null, null, null)).periodo()).isEqualTo(PeriodoEstadistico.MES);
	}

	@ParameterizedTest
	@CsvSource({ "2026-10-20, 2026-10-30, DIA, 11", "2026-08-01, 2026-10-30, SEMANA, 14",
			"2025-10-30, 2026-10-30, MES, 13" })
	void elRangoPersonalizadoEligeLaGranularidadPorSuDuracion(LocalDate desde, LocalDate hasta,
			Granularidad granularidad, int puntos) {
		RangoPeriodo rango = calculadora.rango(new ConsultaPeriodo(PeriodoEstadistico.PERSONALIZADO, desde, hasta));

		assertThat(rango.desde()).isEqualTo(desde);
		assertThat(rango.hasta()).isEqualTo(hasta);
		assertThat(rango.granularidad()).isEqualTo(granularidad);
		assertThat(rango.inicios()).hasSize(puntos);
	}

	@ParameterizedTest
	@CsvSource(value = { "null, 2026-10-30", "2026-10-01, null", "2026-10-20, 2026-10-10", "2026-10-01, 2026-10-31",
			"2025-10-29, 2026-10-30" }, nullValues = "null")
	void rechazaUnRangoPersonalizadoInvalido(LocalDate desde, LocalDate hasta) {
		ConsultaPeriodo consulta = new ConsultaPeriodo(PeriodoEstadistico.PERSONALIZADO, desde, hasta);

		assertThatThrownBy(() -> calculadora.rango(consulta)).isInstanceOf(ReglaNegocioException.class);
	}

}
