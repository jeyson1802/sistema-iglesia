package com.iglesia.seguridad.rol.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Los 4 roles base del sistema: globales, de solo lectura y con permisos fijados por Flyway. Los roles
 * personalizados de cada iglesia heredan de uno de ellos su ámbito y su página de inicio. Los rangos de
 * la iglesia (pastor, diácono…) no son roles: se configuran en iglesia-api.
 */
@Schema(description = "Rol base: `ADMIN_SISTEMA` (todas las iglesias), `ADMIN_IGLESIA` (su iglesia), "
		+ "`LIDER` (su red) o `MIEMBRO` (solo su información).")
public enum RolBase {

	/** Opera sobre todas las iglesias. No pertenece a ninguna. */
	ADMIN_SISTEMA(Ambito.SISTEMA),

	/** Administra una iglesia. */
	ADMIN_IGLESIA(Ambito.IGLESIA),

	/** Ve la información de su red (personas que lidera directa e indirectamente). */
	LIDER(Ambito.RED),

	/** Solo ve su propia información. */
	MIEMBRO(Ambito.PROPIO);

	private final Ambito ambito;

	RolBase(Ambito ambito) {
		this.ambito = ambito;
	}

	/** Sobre qué datos actúan los permisos de este rol y de los roles que heredan de él. */
	public Ambito ambito() {
		return ambito;
	}

	/** Indica si el rol solo tiene sentido vinculado a un miembro de la iglesia. */
	public boolean requiereMiembro() {
		return this == LIDER || this == MIEMBRO;
	}

	/** Indica si un rol personalizado de una iglesia puede heredar de este rol. */
	public boolean heredable() {
		return this != ADMIN_SISTEMA;
	}

}
