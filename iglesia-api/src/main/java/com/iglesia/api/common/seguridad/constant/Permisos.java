package com.iglesia.api.common.seguridad.constant;

/**
 * Códigos de permisos del catálogo de iglesia-seguridad (claim {@code permisos} del JWT) y expresiones de
 * {@code @PreAuthorize} para exigirlos. La autorización se decide siempre por permiso, nunca por rol.
 */
public final class Permisos {

	public static final String IGLESIAS_VER = "iglesias.ver";

	public static final String IGLESIAS_ADMINISTRAR = "iglesias.administrar";

	public static final String PUEDE_VER_IGLESIAS = "hasAuthority('" + IGLESIAS_VER + "')";

	public static final String PUEDE_ADMINISTRAR_IGLESIAS = "hasAuthority('" + IGLESIAS_ADMINISTRAR + "')";

	private Permisos() {
	}

}
