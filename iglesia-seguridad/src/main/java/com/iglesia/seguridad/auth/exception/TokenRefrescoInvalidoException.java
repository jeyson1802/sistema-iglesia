package com.iglesia.seguridad.auth.exception;

/** El token de refresco no existe, expiró, fue revocado o su usuario ya no está activo. */
public class TokenRefrescoInvalidoException extends RuntimeException {

	public TokenRefrescoInvalidoException() {
		super("Token de refresco inválido");
	}

}
