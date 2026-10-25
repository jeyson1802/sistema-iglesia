package com.iglesia.seguridad.auth;

import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;

/**
 * Resultado interno de iniciar sesión o refrescar: el token de acceso va en el cuerpo HTTP y el de
 * refresco en la cookie.
 *
 * @param acceso        token de acceso tal como se responde al cliente
 * @param tokenRefresco token opaco de refresco en claro
 */
record SesionEmitida(TokenAccesoResponse acceso, String tokenRefresco) {

	/** Evita que el token de refresco aparezca en logs o trazas por el toString del record. */
	@Override
	public String toString() {
		return "SesionEmitida[acceso=" + acceso + ", tokenRefresco=***]";
	}

}
