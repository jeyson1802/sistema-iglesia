package com.iglesia.seguridad.common.error;

/** El recurso solicitado no existe o no es visible para el usuario autenticado. */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
