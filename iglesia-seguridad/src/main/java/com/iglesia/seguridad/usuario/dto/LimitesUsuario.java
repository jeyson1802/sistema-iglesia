package com.iglesia.seguridad.usuario.dto;

/** Límites de longitud de los datos de usuario. */
final class LimitesUsuario {

	static final int CORREO_MAX = 254;

	static final int CONTRASENA_MIN = 8;

	/** BCrypt solo considera los primeros 72 bytes. */
	static final int CONTRASENA_MAX = 72;

	private LimitesUsuario() {
	}

}
