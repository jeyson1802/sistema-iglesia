package com.iglesia.api.consolidacion.model;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Reparto balanceado de casos: cada caso va al consolidador que en ese momento tiene menos casos abiertos (contando
 * los que ya tenía antes del reparto); a igual carga, el primero de la lista. Así nadie queda sobrecargado aunque
 * algunos ya tuvieran casos de otras rondas.
 */
public final class Reparto {

	private Reparto() {
	}

	/**
	 * @param casoIds casos por repartir, en el orden en que se asignan
	 * @param cargaActual casos abiertos de cada consolidador elegido, en el orden de preferencia
	 * @return consolidador asignado a cada caso, en el orden de {@code casoIds}
	 */
	public static Map<Long, Long> balanceado(List<Long> casoIds, Map<Long, Long> cargaActual) {
		if (cargaActual.isEmpty()) {
			throw new IllegalArgumentException("Hace falta al menos un consolidador");
		}
		List<Long> orden = List.copyOf(cargaActual.keySet());
		PriorityQueue<Carga> cola = new PriorityQueue<>(
				Comparator.comparingLong(Carga::casos).thenComparingInt(carga -> orden.indexOf(carga.consolidadorId())));
		cargaActual.forEach((consolidadorId, casos) -> cola.add(new Carga(consolidadorId, casos)));
		Map<Long, Long> asignaciones = new LinkedHashMap<>();
		for (Long casoId : casoIds) {
			Carga menor = cola.poll();
			asignaciones.put(casoId, menor.consolidadorId());
			cola.add(new Carga(menor.consolidadorId(), menor.casos() + 1));
		}
		return asignaciones;
	}

	private record Carga(Long consolidadorId, long casos) {
	}

}
