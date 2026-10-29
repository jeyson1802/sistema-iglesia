package com.iglesia.api.miembros.constant;

/** Límites de la ficha del miembro; coinciden con las columnas de la tabla {@code miembro}. */
public final class LimitesMiembro {

	public static final int NOMBRES_MAX = 100;

	public static final int APELLIDOS_MAX = 100;

	public static final int TELEFONO_MAX = 20;

	/** Dígitos, espacios, {@code +} y {@code -}. */
	public static final String TELEFONO_PATRON = "^[0-9 +\\-]*$";

	public static final int CORREO_MAX = 254;

	public static final int DIRECCION_MAX = 200;

	public static final String DISTRITO_PATRON = "^[0-9]{6}$";

	public static final int OCUPACION_MAX = 100;

	public static final int PROFESION_MAX = 100;

	public static final int CENTRO_ESTUDIOS_MAX = 150;

	public static final int PETICION_MAX = 1000;

	public static final int EDAD_MAX = 120;

	public static final int BUSQUEDA_MAX = 100;

	/** Máximo de sugerencias del selector de miembro. */
	public static final int OPCIONES_MAX = 20;

	private LimitesMiembro() {
	}

}
