package com.iglesia.seguridad.common.periodo.component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import org.springframework.stereotype.Component;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.seguridad.common.periodo.model.PeriodoEstadistico;
import com.iglesia.seguridad.common.periodo.model.RangoPeriodo;

/** Convierte un periodo del selector en fechas, con el día de hoy en la zona de la aplicación. */
@Component
public class CalculadoraPeriodo {

	/** Días máximos de un rango personalizado (los mismos que el filtro de aportes). */
	public static final int DIAS_MAXIMOS_PERSONALIZADO = 366;

	private final Clock reloj;

	public CalculadoraPeriodo(Clock reloj) {
		this.reloj = reloj;
	}

	public LocalDate hoy() {
		return LocalDate.ofInstant(reloj.instant(), ZoneId.systemDefault());
	}

	public RangoPeriodo rango(ConsultaPeriodo consulta) {
		PeriodoEstadistico periodo = consulta.periodoPedido();
		LocalDate hoy = hoy();
		return switch (periodo) {
			case HOY -> RangoPeriodo.de(periodo, hoy, hoy);
			case SEMANA -> RangoPeriodo.de(periodo, hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), hoy);
			case MES -> RangoPeriodo.de(periodo, hoy.withDayOfMonth(1), hoy);
			case MES_ANTERIOR -> {
				LocalDate finMesAnterior = hoy.withDayOfMonth(1).minusDays(1);
				yield RangoPeriodo.de(periodo, finMesAnterior.withDayOfMonth(1), finMesAnterior);
			}
			case ANIO -> RangoPeriodo.de(periodo, hoy.withDayOfYear(1), hoy);
			case PERSONALIZADO -> personalizado(consulta.desde(), consulta.hasta(), hoy);
		};
	}

	private static RangoPeriodo personalizado(LocalDate desde, LocalDate hasta, LocalDate hoy) {
		if (desde == null || hasta == null) {
			throw new ReglaNegocioException("Indica el primer y el último día del rango personalizado.");
		}
		if (hasta.isBefore(desde)) {
			throw new ReglaNegocioException("El último día del rango no puede ser anterior al primero.");
		}
		if (hasta.isAfter(hoy)) {
			throw new ReglaNegocioException("El rango no puede terminar después de hoy.");
		}
		if (ChronoUnit.DAYS.between(desde, hasta) + 1 > DIAS_MAXIMOS_PERSONALIZADO) {
			throw new ReglaNegocioException("Elige un rango de hasta " + DIAS_MAXIMOS_PERSONALIZADO + " días.");
		}
		return RangoPeriodo.de(PeriodoEstadistico.PERSONALIZADO, desde, hasta);
	}

}
