package com.iglesia.api.miembros.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoAsistenciaTest {

	@ParameterizedTest
	@ValueSource(strings = { "ASIS:7K2QM9XDR4TB", "7K2QM9XDR4TB", "asis:7k2qm9xdr4tb", "  7K2Q M9XD R4TB ",
			"7K2Q-M9XD-R4TB" })
	void leeElCodigoConOSinPrefijoMinusculasYSeparadores(String leido) {
		assertThat(CodigoAsistencia.leer(leido)).contains(new CodigoAsistencia("7K2QM9XDR4TB"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://pago.example/qr", "ASIS:7K2QM9XDR4T", "ASIS:7K2QM9XDR4TBX", "7K2QM9XDR4TI",
			"7K2QM9XDR4TL", "7K2QM9XDR4TO", "7K2QM9XDR4TU", "", "   " })
	void rechazaLoQueNoEsUnCodigoDeAsistencia(String leido) {
		assertThat(CodigoAsistencia.leer(leido)).isEmpty();
	}

	@Test
	void sinTextoNoHayCodigo() {
		assertThat(CodigoAsistencia.leer(null)).isEmpty();
	}

	@Test
	void elQrLlevaElPrefijo() {
		assertThat(new CodigoAsistencia("7K2QM9XDR4TB").contenidoQr()).isEqualTo("ASIS:7K2QM9XDR4TB");
	}

	@Test
	void noSePuedeConstruirConUnFormatoInvalido() {
		assertThatThrownBy(() -> new CodigoAsistencia("corto")).isInstanceOf(IllegalArgumentException.class);
	}

}
