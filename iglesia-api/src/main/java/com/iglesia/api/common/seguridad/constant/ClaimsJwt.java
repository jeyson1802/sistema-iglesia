package com.iglesia.api.common.seguridad.constant;

/** Claims propios del token emitido por iglesia-seguridad (debe coincidir con ese servicio). */
public final class ClaimsJwt {

	public static final String ROLES = "roles";

	public static final String PERMISOS = "permisos";

	public static final String IGLESIA_ID = "iglesia_id";

	public static final String MIEMBRO_ID = "miembro_id";

	/** {@code true} si el usuario tiene una contraseña temporal que debe cambiar antes de usar el sistema. */
	public static final String CAMBIO_CONTRASENA = "cambio_contrasena";

	private ClaimsJwt() {
	}

}
