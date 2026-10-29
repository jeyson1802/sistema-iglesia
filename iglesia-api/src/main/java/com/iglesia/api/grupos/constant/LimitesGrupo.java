package com.iglesia.api.grupos.constant;

/** Límites de los datos de grupos y de sus nombres configurables; coinciden con las columnas de la base. */
public final class LimitesGrupo {

	public static final int NOMBRE_MAX = 100;

	public static final int DIRECCION_MAX = 200;

	public static final int DESCRIPCION_MAX = 500;

	public static final String DISTRITO_PATRON = "^[0-9]{6}$";

	public static final int NOMBRE_TIPO_MAX = 40;

	/** Nombre del tipo de grupo cuando la iglesia no configuró el suyo. */
	public static final String NOMBRE_SINGULAR_POR_DEFECTO = "Grupo";

	public static final String NOMBRE_PLURAL_POR_DEFECTO = "Grupos";

	private LimitesGrupo() {
	}

}
