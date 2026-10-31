package com.iglesia.api.common.estadisticas.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.iglesia.api.common.periodo.model.RangoPeriodo;

/** Convierte las filas agrupadas por día, semana o mes en una serie sin huecos (los puntos sin filas valen cero). */
public final class Series {

	private Series() {
	}

	/**
	 * @param filas  filas agrupadas por inicio de día, semana o mes
	 * @param inicio inicio de cada fila
	 * @param punto  crea el punto a partir del inicio y la fila, que es {@code null} si no hubo datos
	 */
	public static <F, P> List<P> rellenar(RangoPeriodo rango, List<F> filas, Function<F, LocalDate> inicio,
			PuntoDeFila<F, P> punto) {
		Map<LocalDate, F> porInicio = filas.stream().collect(Collectors.toMap(inicio, Function.identity()));
		return rango.inicios().stream().map(fecha -> punto.crear(fecha, porInicio.get(fecha))).toList();
	}

	@FunctionalInterface
	public interface PuntoDeFila<F, P> {

		P crear(LocalDate inicio, F fila);

	}

}
