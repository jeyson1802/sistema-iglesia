package com.iglesia.api.common.seguridad.constant;

/**
 * Códigos de los roles base de iglesia-seguridad que llegan en el claim {@code roles}. Solo se usan para decidir el
 * ámbito de lectura (qué filas ve cada uno); qué operaciones puede hacer se decide siempre por permiso.
 */
public final class RolesBase {

	public static final String ADMIN_SISTEMA = "ADMIN_SISTEMA";

	public static final String ADMIN_IGLESIA = "ADMIN_IGLESIA";

	public static final String LIDER = "LIDER";

	private RolesBase() {
	}

}
