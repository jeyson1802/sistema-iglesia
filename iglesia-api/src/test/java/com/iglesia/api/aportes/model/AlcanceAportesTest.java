package com.iglesia.api.aportes.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlcanceAportesTest {

	@Test
	void elAlcanceCompletoRespetaElFiltroPedidoEIncluyeATodos() {
		AlcanceAportes completo = AlcanceAportes.completo();

		assertThat(completo.aportanteEfectivo(5L)).isEqualTo(5L);
		assertThat(completo.aportanteEfectivo(null)).isNull();
		assertThat(completo.incluye(null)).isTrue();
		assertThat(completo.vacio()).isFalse();
	}

	@Test
	void elAlcancePropioIgnoraElFiltroYSoloIncluyeAlMiembro() {
		AlcanceAportes propio = AlcanceAportes.soloDe(3L);

		assertThat(propio.aportanteEfectivo(9L)).isEqualTo(3L);
		assertThat(propio.incluye(3L)).isTrue();
		assertThat(propio.incluye(9L)).isFalse();
		assertThat(propio.incluye(null)).isFalse();
	}

	@Test
	void sinAlcanceNoIncluyeANadie() {
		assertThat(AlcanceAportes.ninguno().vacio()).isTrue();
		assertThat(AlcanceAportes.ninguno().incluye(null)).isFalse();
	}

}
