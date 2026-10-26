package com.iglesia.seguridad.jwt;

/** Nombres de los claims propios incluidos en el token de acceso (contrato con iglesia-api y el frontend). */
public final class ClaimsJwt {

	public static final String ROLES = "roles";

	public static final String IGLESIA_ID = "iglesia_id";

	public static final String MIEMBRO_ID = "miembro_id";

	private ClaimsJwt() {
	}

}
