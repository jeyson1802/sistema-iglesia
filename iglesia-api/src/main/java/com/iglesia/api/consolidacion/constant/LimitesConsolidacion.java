package com.iglesia.api.consolidacion.constant;

/** Límites y valores fijos de la consolidación (validaciones, ventanas de tiempo y tamaños). */
public final class LimitesConsolidacion {

	public static final int NOMBRE_RONDA_MIN = 3;

	public static final int NOMBRE_RONDA_MAX = 80;

	public static final int DESCRIPCION_RONDA_MAX = 500;

	public static final int NOMBRE_RESULTADO_MIN = 2;

	public static final int NOMBRE_RESULTADO_MAX = 60;

	public static final int OBSERVACIONES_MAX = 1000;

	/** Horas durante las que un consolidador puede corregir o borrar su propio contacto. */
	public static final int HORAS_EDICION_CONTACTO = 24;

	/** Minutos de diferencia de reloj del celular que se toleran en la fecha de un contacto. */
	public static final int TOLERANCIA_RELOJ_MINUTOS = 5;

	/** Días después del primer contacto en que una asistencia cuenta como «volvió a asistir». */
	public static final int DIAS_VOLVIO_A_ASISTIR = 28;

	/** Días hacia atrás de la actividad reciente de un consolidador y de las fechas de contacto de una persona. */
	public static final int DIAS_ACTIVIDAD_RECIENTE = 30;

	/** Semanas de la evolución de asistencia en que se marcan los contactos. */
	public static final int SEMANAS_CONTACTOS_RECIENTES = 12;

	/** Tope de personas que se agregan a una ronda de una vez. */
	public static final int LOTE_MAX = 1000;

	private LimitesConsolidacion() {
	}

}
