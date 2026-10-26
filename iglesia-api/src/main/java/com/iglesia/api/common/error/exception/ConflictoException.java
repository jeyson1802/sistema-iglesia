package com.iglesia.api.common.error.exception;

/** La operación choca con el estado actual (p. ej. un dato que debe ser único ya existe). */
public class ConflictoException extends RuntimeException {

	public ConflictoException(String mensaje) {
		super(mensaje);
	}

}
