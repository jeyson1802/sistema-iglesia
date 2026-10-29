package com.iglesia.api.aportes.constant;

/** Límites de los aportes y sus categorías; coinciden con las columnas de {@code aporte} y {@code categoria_aporte}. */
public final class LimitesAporte {

	public static final int NOMBRE_CATEGORIA_MAX = 60;

	public static final int MONTO_ENTEROS = 10;

	public static final int MONTO_DECIMALES = 2;

	public static final int NUMERO_OPERACION_MAX = 40;

	public static final int PETICION_MAX = 1000;

	public static final int OBSERVACION_MAX = 500;

	public static final int MOTIVO_ANULACION_MAX = 300;

	/** Rango máximo de una consulta de aportes, para que el listado y los totales sigan siendo rápidos. */
	public static final int RANGO_MAX_DIAS = 366;

	private LimitesAporte() {
	}

}
