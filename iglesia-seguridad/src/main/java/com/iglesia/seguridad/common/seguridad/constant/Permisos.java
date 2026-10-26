package com.iglesia.seguridad.common.seguridad.constant;

/**
 * Códigos del catálogo fijo de permisos (tabla {@code permiso}, sembrada por Flyway) y expresiones de
 * {@code @PreAuthorize} para exigirlos. La autorización se decide siempre por permiso, nunca por rol.
 */
public final class Permisos {

	public static final String USUARIOS_VER = "usuarios.ver";

	public static final String USUARIOS_ADMINISTRAR = "usuarios.administrar";

	public static final String ROLES_VER = "roles.ver";

	public static final String ROLES_ADMINISTRAR = "roles.administrar";

	public static final String PUEDE_VER_USUARIOS = "hasAuthority('" + USUARIOS_VER + "')";

	public static final String PUEDE_ADMINISTRAR_USUARIOS = "hasAuthority('" + USUARIOS_ADMINISTRAR + "')";

	public static final String PUEDE_VER_ROLES = "hasAuthority('" + ROLES_VER + "')";

	public static final String PUEDE_ADMINISTRAR_ROLES = "hasAuthority('" + ROLES_ADMINISTRAR + "')";

	private Permisos() {
	}

}
