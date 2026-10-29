package com.iglesia.api.asistencia.model;

import java.time.LocalDate;

import com.iglesia.api.asistencia.constant.LimitesSeguimiento;

/**
 * Alertas de seguimiento. Cada valor es la regla que decide si aplica (patrón Strategy) y el orden de declaración es
 * su gravedad: primero la más grave.
 */
public enum TipoAlerta {

	/** Las últimas N ocasiones que le correspondían, seguidas, sin asistir. */
	FALTAS_SEGUIDAS {
		@Override
		public boolean aplica(IndicadoresMiembro indicadores, ReglasSeguimiento reglas, LocalDate hoy) {
			return indicadores.faltasSeguidas() >= reglas.faltasSeguidas();
		}
	},

	/** Nuevo (por condición o por fecha de ingreso) que vino pocas veces y no volvió en las últimas dos semanas. */
	NUEVO_SIN_VOLVER {
		@Override
		public boolean aplica(IndicadoresMiembro indicadores, ReglasSeguimiento reglas, LocalDate hoy) {
			LocalDate limiteSinVolver = hoy.minusDays(LimitesSeguimiento.DIAS_SIN_VOLVER);
			boolean esNuevo = indicadores.nuevo() || !indicadores.inicio().isBefore(hoy.minusDays(reglas.diasNuevo()));
			boolean tuvoTiempoDeVolver = !indicadores.inicio().isAfter(limiteSinVolver);
			boolean noVolvio = indicadores.ultimaAsistencia() == null
					|| indicadores.ultimaAsistencia().isBefore(limiteSinVolver);
			return esNuevo && tuvoTiempoDeVolver && noVolvio
					&& indicadores.diasAsistidos() <= LimitesSeguimiento.VISITAS_NUEVO_MAX;
		}
	},

	/** Su porcentaje de las últimas 4 semanas bajó al menos P puntos frente a las 8 anteriores, donde venía bien. */
	ASISTENCIA_EN_CAIDA {
		@Override
		public boolean aplica(IndicadoresMiembro indicadores, ReglasSeguimiento reglas, LocalDate hoy) {
			Integer reciente = indicadores.ultimas4().porcentaje();
			Integer base = indicadores.semanas5a12().porcentaje();
			return reciente != null && base != null && base >= LimitesSeguimiento.PORCENTAJE_BASE_CAIDA_MIN
					&& base - reciente >= reglas.puntosCaida();
		}
	};

	public abstract boolean aplica(IndicadoresMiembro indicadores, ReglasSeguimiento reglas, LocalDate hoy);

}
