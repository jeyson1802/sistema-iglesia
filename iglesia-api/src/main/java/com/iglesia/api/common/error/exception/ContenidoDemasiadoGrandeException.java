package com.iglesia.api.common.error.exception;

/** El archivo enviado supera el tamaño máximo admitido; responde 413 con el límite en el mensaje. */
public class ContenidoDemasiadoGrandeException extends RuntimeException {

	public ContenidoDemasiadoGrandeException(String mensaje) {
		super(mensaje);
	}

}
