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

	/** Solo del rol base {@code ADMIN_SISTEMA}; el catálogo de iglesias lo expone iglesia-api. */
	public static final String IGLESIAS_VER = "iglesias.ver";

	/** Solo del rol base {@code ADMIN_SISTEMA}: dar de alta iglesias y entrar a operarlas. */
	public static final String IGLESIAS_ADMINISTRAR = "iglesias.administrar";

	public static final String PUEDE_VER_USUARIOS ="hasAuthority('" + USUARIOS_VER + "')";

	public static final String PUEDE_ADMINISTRAR_USUARIOS = "hasAuthority('" + USUARIOS_ADMINISTRAR + "')";

	public static final String PUEDE_VER_ROLES = "hasAuthority('" + ROLES_VER + "')";

	/** Solo para listar roles: quien asigna roles a usuarios necesita verlos, aunque no pueda editarlos. */
	public static final String PUEDE_VER_ROLES_O_ASIGNARLOS = "hasAnyAuthority('" + ROLES_VER + "', '"
			+ USUARIOS_ADMINISTRAR + "')";

	public static final String PUEDE_ADMINISTRAR_ROLES = "hasAuthority('" + ROLES_ADMINISTRAR + "')";

	public static final String PUEDE_ADMINISTRAR_IGLESIAS = "hasAuthority('" + IGLESIAS_ADMINISTRAR + "')";

	private Permisos() {
	}

}
