package com.iglesia.seguridad.usuario.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** Roles de acceso al sistema. Son técnicos y fijos; los rangos de la iglesia se configuran en iglesia-api. */
@Schema(description = "Rol de acceso: `ADMIN_SISTEMA` (todas las iglesias), `ADMIN_IGLESIA` (su iglesia), "
		+ "`LIDER` (su red) o `MIEMBRO` (solo su información).")
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
