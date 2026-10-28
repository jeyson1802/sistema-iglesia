package com.iglesia.seguridad.common.seguridad.exception;

import org.springframework.security.access.AccessDeniedException;

/** El usuario tiene una contraseña temporal y debe cambiarla antes de usar cualquier otra operación. */
public class CambioContrasenaPendienteException extends AccessDeniedException {

	public static final String DETALLE = "Debes cambiar tu contraseña temporal antes de continuar";

	public CambioContrasenaPendienteException() {
		super(DETALLE);
	}

}
