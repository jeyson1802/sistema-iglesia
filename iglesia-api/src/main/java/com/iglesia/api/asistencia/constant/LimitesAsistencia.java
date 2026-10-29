package com.iglesia.api.asistencia.constant;

/** Límites de la asistencia; coinciden con la migración {@code V9__asistencia.sql}. */
public final class LimitesAsistencia {

	public static final int NOMBRE_CULTO_MIN = 2;

	public static final int NOMBRE_CULTO_MAX = 60;

	public static final int NOMBRE_EVENTO_MIN = 2;

	public static final int NOMBRE_EVENTO_MAX = 100;

	/** Un evento dura como mucho una semana: una reunión por día. */
	public static final int DIAS_EVENTO_MAX = 7;

	/** Semanas que cuenta el porcentaje de asistencia de un miembro. */
	public static final int SEMANAS_RESUMEN = 8;

	/** Últimas reuniones a las que asistió un miembro que se muestran en su ficha. */
	public static final int ULTIMAS_REUNIONES = 5;

	/** Miembros que devuelve el buscador de la toma de asistencia manual. */
	public static final int BUSQUEDA_MAX = 20;

	public static final int BUSQUEDA_MIN = 2;

	/** El código admite separadores al escribirlo a mano; más largo que esto no es un código. */
	public static final int CODIGO_LEIDO_MAX = 40;

	private LimitesAsistencia() {
	}

}
