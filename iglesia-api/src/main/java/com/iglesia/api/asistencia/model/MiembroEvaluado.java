package com.iglesia.api.asistencia.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Indicadores de un miembro con las alertas que le aplican según las reglas de la iglesia. */
public record MiembroEvaluado(IndicadoresMiembro indicadores, List<TipoAlerta> alertas) {

	/** Primero los de alerta más grave; a igual alerta, más faltas seguidas y menor porcentaje. */
	public static final Comparator<MiembroEvaluado> POR_RIESGO = Comparator
		.comparingInt(MiembroEvaluado::gravedad)
		.thenComparing(evaluado -> evaluado.indicadores().faltasSeguidas(), Comparator.reverseOrder())
		.thenComparing(evaluado -> evaluado.indicadores().ultimas8().porcentaje(),
				Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(evaluado -> evaluado.indicadores().miembroId());

	public MiembroEvaluado {
		alertas = List.copyOf(alertas);
	}

	public static MiembroEvaluado evaluar(IndicadoresMiembro indicadores, ReglasSeguimiento reglas, LocalDate hoy) {
		return new MiembroEvaluado(indicadores, Arrays.stream(TipoAlerta.values())
			.filter(alerta -> alerta.aplica(indicadores, reglas, hoy))
			.toList());
	}

	public boolean tiene(TipoAlerta alerta) {
		return alertas.contains(alerta);
	}

	/** Ordinal de su alerta más grave; sin alertas, después de todas. */
	private int gravedad() {
		return alertas.isEmpty() ? TipoAlerta.values().length : alertas.getFirst().ordinal();
	}

}
