package com.iglesia.seguridad.rol.model;

/**
 * Alcance de los datos sobre los que actúan los permisos de un rol. No se guarda: se deriva del rol base
 * ({@link RolBase#ambito()}), para no multiplicar permisos como {@code miembros.ver_red}.
 */
public enum Ambito {

	/** Todas las iglesias. */
	SISTEMA,

	/** La iglesia del usuario. */
	IGLESIA,

	/** Las personas que el usuario lidera, directa e indirectamente. */
	RED,

	/** Solo la información del propio usuario. */
	PROPIO

}
