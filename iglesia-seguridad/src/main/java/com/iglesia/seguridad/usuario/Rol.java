package com.iglesia.seguridad.usuario;

/** Roles de acceso al sistema. Son técnicos y fijos; los rangos de la iglesia se configuran en iglesia-api. */
public enum Rol {

	/** Opera sobre todas las iglesias. No pertenece a ninguna. */
	ADMIN_SISTEMA,

	/** Administra una iglesia. */
	ADMIN_IGLESIA,

	/** Ve la información de su red (personas que lidera directa e indirectamente). */
	LIDER,

	/** Solo ve su propia información. */
	MIEMBRO;

	/** Indica si el rol solo tiene sentido vinculado a un miembro de la iglesia. */
	public boolean requiereMiembro() {
		return this == LIDER || this == MIEMBRO;
	}

}
