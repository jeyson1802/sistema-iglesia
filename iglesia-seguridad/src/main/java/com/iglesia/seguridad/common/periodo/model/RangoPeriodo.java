package com.iglesia.seguridad.common.periodo.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Fechas de un periodo y de su periodo anterior, de la misma duración y que termina el día antes de {@code desde}.
 * Todas las fechas son inclusivas. La granularidad de las series depende de la duración.
 */
public record RangoPeriodo(PeriodoEstadistico periodo, LocalDate desde, LocalDate hasta, LocalDate anteriorDesde,
		LocalDate anteriorHasta, Granularidad granularidad) {

	public static RangoPeriodo de(PeriodoEstadistico periodo, LocalDate desde, LocalDate hasta) {
		long dias = ChronoUnit.DAYS.between(desde, hasta);
		LocalDate anteriorHasta = desde.minusDays(1);
		return new RangoPeriodo(periodo, desde, hasta, anteriorHasta.minusDays(dias), anteriorHasta,
				Granularidad.para(dias + 1));
	}

	/** Días del periodo, contando {@code desde} y {@code hasta}. */
	public long dias() {
		return ChronoUnit.DAYS.between(desde, hasta) + 1;
	}

	/** Inicio de cada punto de la serie (el día, el lunes o el día 1), del más antiguo al más reciente. */
	public List<LocalDate> inicios() {
		List<LocalDate> inicios = new ArrayList<>();
		LocalDate inicio = switch (granularidad) {
			case DIA -> desde;
			case SEMANA -> desde.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case MES -> desde.withDayOfMonth(1);
		};
		while (!inicio.isAfter(hasta)) {
			inicios.add(inicio);
			inicio = switch (granularidad) {
				case DIA -> inicio.plusDays(1);
				case SEMANA -> inicio.plusWeeks(1);
				case MES -> inicio.plusMonths(1);
			};
		}
		return inicios;
	}

}
