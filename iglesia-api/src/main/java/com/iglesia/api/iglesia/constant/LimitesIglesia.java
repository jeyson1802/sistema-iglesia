package com.iglesia.api.iglesia.constant;

/** Límites de los datos de una iglesia; coinciden con las columnas de la tabla {@code iglesia}. */
public final class LimitesIglesia {

	/** Corto para que quepa en la topbar. */
	public static final int NOMBRE_COMERCIAL_MIN = 2;

	public static final int NOMBRE_COMERCIAL_MAX = 60;

	public static final int DENOMINACION_MAX = 200;

	public static final int DIRECCION_MAX = 200;

	public static final int TELEFONO_MAX = 20;

	/** Dígitos, espacios, {@code +}, {@code -} y paréntesis; vacío equivale a no indicarlo. */
	public static final String TELEFONO_PATRON = "^$|^[+(0-9][0-9 ()+-]*$";

	public static final int CORREO_MAX = 120;

	public static final int SITIO_WEB_MAX = 200;

	/** Vacío equivale a no indicarlo. */
	public static final String SITIO_WEB_PATRON = "^$|^https?://\\S+$";

	public static final int BUSQUEDA_MAX = 100;

	/** Código INEI de departamento (2 dígitos), provincia (4) o distrito (6). */
	public static final String UBIGEO_PATRON = "^(\\d{2}|\\d{4}|\\d{6})$";

	public static final String DISTRITO_PATRON = "^\\d{6}$";

	/** 1 MB: suficiente para un logo nítido en la topbar sin cargar la base. */
	public static final int LOGO_MAX_BYTES = 1024 * 1024;

	private LimitesIglesia() {
	}

}
