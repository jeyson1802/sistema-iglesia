package com.iglesia.api.common.error;

/** La petición es válida en formato pero incumple una regla del dominio. */
public class ReglaNegocioException extends RuntimeException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}

}
