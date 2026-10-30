package com.iglesia.api.consolidacion.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepartoTest {

	@Test
	void sinCargaPreviaRepartePorTurnosEnElOrdenDeLosConsolidadores() {
		Map<Long, Long> carga = new LinkedHashMap<>();
		carga.put(10L, 0L);
		carga.put(20L, 0L);

		assertThat(Reparto.balanceado(List.of(1L, 2L, 3L, 4L, 5L), carga))
			.containsExactly(Map.entry(1L, 10L), Map.entry(2L, 20L), Map.entry(3L, 10L), Map.entry(4L, 20L),
					Map.entry(5L, 10L));
	}

	@Test
	void quienYaTieneCasosRecibeMenosHastaIgualarse() {
		Map<Long, Long> carga = new LinkedHashMap<>();
		carga.put(10L, 3L);
		carga.put(20L, 0L);

		Map<Long, Long> asignaciones = Reparto.balanceado(List.of(1L, 2L, 3L, 4L, 5L), carga);

		assertThat(asignaciones.values().stream().filter(id -> id == 20L).count()).isEqualTo(4);
		assertThat(asignaciones.values().stream().filter(id -> id == 10L).count()).isEqualTo(1);
	}

	@Test
	void sinConsolidadoresNoSePuedeRepartir() {
		assertThatThrownBy(() -> Reparto.balanceado(List.of(1L), Map.of()))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
