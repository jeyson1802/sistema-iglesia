package com.iglesia.api.asistencia.model;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static com.iglesia.api.asistencia.model.IndicadoresPrueba.miembro;
import static org.assertj.core.api.Assertions.assertThat;

class MiembroEvaluadoTest {

	@Test
	void ordenaPorAlertaMasGraveLuegoFaltasSeguidasYPorcentaje() {
		var sinAlertas = new MiembroEvaluado(miembro().id(1).crear(), List.of());
		var caida = new MiembroEvaluado(miembro().id(2).crear(), List.of(TipoAlerta.ASISTENCIA_EN_CAIDA));
		var tresFaltas = new MiembroEvaluado(miembro().id(3).faltasSeguidas(3).crear(),
				List.of(TipoAlerta.FALTAS_SEGUIDAS));
		var seisFaltas = new MiembroEvaluado(miembro().id(4).faltasSeguidas(6).crear(),
				List.of(TipoAlerta.FALTAS_SEGUIDAS));

		var ordenados = Stream.of(sinAlertas, caida, tresFaltas, seisFaltas).sorted(MiembroEvaluado.POR_RIESGO).toList();

		assertThat(ordenados).containsExactly(seisFaltas, tresFaltas, caida, sinAlertas);
	}

	@Test
	void elPorcentajeSinOcasionesEsSinDatos() {
		assertThat(new Asistidas(0, 0).porcentaje()).isNull();
		assertThat(new Asistidas(3, 2).porcentaje()).isEqualTo(67);
		assertThat(new Asistidas(4, 4).porcentaje()).isEqualTo(100);
	}

}
