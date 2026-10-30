package com.iglesia.seguridad.common.seguridad.constant;

/**
 * Códigos del catálogo fijo de permisos (tabla {@code permiso}, sembrada por Flyway) y expresiones de
 * {@code @PreAuthorize} para exigirlos. La autorización se decide siempre por permiso, nunca por rol.
 */
public final class Permisos {

	public static final String MIEMBROS_VER = "miembros.ver";

	public static final String NIVELES_ADMINISTRAR = "niveles.administrar";

	public static final String GRUPOS_VER = "grupos.ver";

	public static final String GRUPOS_ADMINISTRAR = "grupos.administrar";

	public static final String APORTES_VER = "aportes.ver";

	public static final String APORTES_REGISTRAR = "aportes.registrar";

	public static final String APORTES_EDITAR = "aportes.editar";

	public static final String APORTES_ANULAR = "aportes.anular";

	public static final String APORTES_CONFIGURAR = "aportes.configurar";

	public static final String IGLESIA_PERSONALIZAR = "iglesia.personalizar";

	public static final String ASISTENCIA_REGISTRAR = "asistencia.registrar";

	public static final String ASISTENCIA_GRUPO = "asistencia.grupo";

	public static final String ASISTENCIA_VER = "asistencia.ver";

	public static final String ASISTENCIA_CONFIGURAR = "asistencia.configurar";

	public static final String CONSOLIDACION_ADMINISTRAR = "consolidacion.administrar";

	public static final String CONSOLIDACION_VER = "consolidacion.ver";

	public static final String CONSOLIDACION_CONTACTAR = "consolidacion.contactar";

	public static final String USUARIOS_VER = "usuarios.ver";

	public static final String USUARIOS_ADMINISTRAR = "usuarios.administrar";

	public static final String ROLES_VER = "roles.ver";

	public static final String ROLES_ADMINISTRAR = "roles.administrar";

	/** Solo del rol base {@code ADMIN_SISTEMA}; el catálogo de iglesias lo expone iglesia-api. */
	public static final String IGLESIAS_VER = "iglesias.ver";

	/** Solo del rol base {@code ADMIN_SISTEMA}: dar de alta iglesias y entrar a operarlas. */
	public static final String IGLESIAS_ADMINISTRAR = "iglesias.administrar";

	public static final String PUEDE_VER_USUARIOS = "hasAuthority('" + USUARIOS_VER + "')";

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
