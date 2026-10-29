package com.iglesia.api.miembros.component;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.iglesia.api.miembros.model.CodigoAsistencia;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCodigoAsistenciaTest {

	private final GeneradorCodigoAsistencia generador = new GeneradorCodigoAsistencia();

	@Test
	void generaCodigosValidosYDistintos() {
		Set<String> generados = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			CodigoAsistencia codigo = generador.generar();
			assertThat(codigo.valor()).hasSize(CodigoAsistencia.LONGITUD).matches("[0-9A-HJKMNP-TV-Z]+");
			generados.add(codigo.valor());
		}
		assertThat(generados).hasSize(1000);
	}

}
