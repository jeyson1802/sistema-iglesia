package com.iglesia.seguridad.usuario.constant;

/** Límites de longitud de los datos de usuario. */
public final class LimitesUsuario {

	public static final int CORREO_MAX = 254;

	public static final int CONTRASENA_MIN = 8;

	/** BCrypt solo considera los primeros 72 bytes. */
	public static final int CONTRASENA_MAX = 72;

	private LimitesUsuario() {
	}

}
