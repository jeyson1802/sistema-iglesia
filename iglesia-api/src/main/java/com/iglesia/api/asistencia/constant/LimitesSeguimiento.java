package com.iglesia.api.asistencia.constant;

/**
 * Ventanas y límites del seguimiento de asistencia; los límites de las reglas coinciden con la migración
 * {@code V10__seguimiento_de_asistencia.sql}. Las semanas van de lunes a domingo y la semana 0 es la actual.
 */
public final class LimitesSeguimiento {

	/** Semanas que se analizan: las 8 del porcentaje y las 8 anteriores, para la tendencia. */
	public static final int SEMANAS_ANALISIS = 16;

	/** Semanas que muestra la evolución (ficha, grupo y servicios). */
	public static final int SEMANAS_EVOLUCION = 12;

	/** La alerta de caída compara las últimas 4 semanas con las 8 anteriores. */
	public static final int SEMANAS_RECIENTES_CAIDA = 4;

	public static final int SEMANAS_BASE_CAIDA = 8;

	/** Para la alerta de caída, en las semanas base tenía al menos este porcentaje. */
	public static final int PORCENTAJE_BASE_CAIDA_MIN = 50;

	/** Un nuevo «no volvió» si no asistió en estos días y, como mucho, vino {@link #VISITAS_NUEVO_MAX} días. */
	public static final int DIAS_SIN_VOLVER = 14;

	public static final int VISITAS_NUEVO_MAX = 2;

	public static final int FALTAS_SEGUIDAS_POR_DEFECTO = 3;

	public static final int FALTAS_SEGUIDAS_MIN = 2;

	public static final int FALTAS_SEGUIDAS_MAX = 10;

	public static final int PUNTOS_CAIDA_POR_DEFECTO = 30;

	public static final int PUNTOS_CAIDA_MIN = 10;

	public static final int PUNTOS_CAIDA_MAX = 80;

	public static final int DIAS_NUEVO_POR_DEFECTO = 90;

	public static final int DIAS_NUEVO_MIN = 30;

	public static final int DIAS_NUEVO_MAX = 180;

	private LimitesSeguimiento() {
	}

}
