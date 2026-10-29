package com.iglesia.api.grupos.model;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaSemanaTest {

	@Test
	void convierteElDiaIsoAlDiaDeLaIglesia() {
		assertThat(DiaSemana.de(DayOfWeek.MONDAY)).isEqualTo(DiaSemana.LUNES);
		assertThat(DiaSemana.de(DayOfWeek.WEDNESDAY)).isEqualTo(DiaSemana.MIERCOLES);
		assertThat(DiaSemana.de(DayOfWeek.SUNDAY)).isEqualTo(DiaSemana.DOMINGO);
	}

}
