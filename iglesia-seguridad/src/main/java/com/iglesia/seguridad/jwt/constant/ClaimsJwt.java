package com.iglesia.seguridad.jwt.constant;

/** Nombres de los claims propios incluidos en el token de acceso (contrato con iglesia-api y el frontend). */
public final class ClaimsJwt {

	public static final String ROLES = "roles";

	public static final String PERMISOS = "permisos";

	public static final String IGLESIA_ID = "iglesia_id";

	public static final String MIEMBRO_ID = "miembro_id";

	/** {@code true} mientras el usuario tenga una contraseña temporal que debe cambiar. Se emite siempre. */
	public static final String CAMBIO_CONTRASENA = "cambio_contrasena";

	/**
	 * {@code true} cuando el administrador del sistema opera dentro de la iglesia de {@link #IGLESIA_ID}. Solo se
	 * emite en ese caso; su rol sigue siendo {@code ADMIN_SISTEMA}.
	 */
	public static final String ACTUANDO_EN_IGLESIA = "actuando_en_iglesia";

	private ClaimsJwt() {
	}

}
