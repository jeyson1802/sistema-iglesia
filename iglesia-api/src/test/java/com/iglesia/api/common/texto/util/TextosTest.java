package com.iglesia.api.common.texto.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextosTest {

	@Test
	void normalizaEspaciosSobrantes() {
		assertThat(Textos.normalizar("  Juan   Carlos ")).isEqualTo("Juan Carlos");
		assertThat(Textos.normalizarOpcional("   ")).isNull();
		assertThat(Textos.normalizarOpcional(null)).isNull();
	}

	@Test
	void recortaSoloLosExtremosYConservaLosSaltosDeLinea() {
		assertThat(Textos.recortarOpcional("  Por mi familia\n  y mi trabajo  ")).isEqualTo("Por mi familia\n  y mi trabajo");
		assertThat(Textos.recortarOpcional(" \n ")).isNull();
		assertThat(Textos.recortarOpcional(null)).isNull();
	}

	@Test
	void comparaSinDistinguirMayusculasNiTildesEnMayuscula() {
		assertThat(Textos.mismoTexto("PÉREZ ", "pérez")).isTrue();
		assertThat(Textos.mismoTexto("Líder", "  LÍDER")).isTrue();
		assertThat(Textos.mismoTexto("Peña", "Pena")).isFalse();
	}

	@Test
	void elPatronIgnoraTildesYMayusculas() {
		assertThat(Textos.patronContiene("PÉREZ Muñoz")).isEqualTo("%perez munoz%");
	}

	@Test
	void elPatronBuscaLosComodinesLiteralmente() {
		assertThat(Textos.patronContiene(" Ana_% ")).isEqualTo("%ana\\_\\%%");
	}

}
