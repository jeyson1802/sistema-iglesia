package com.iglesia.api.common.error.exception;

import java.util.Optional;

/**
 * La operación choca con el estado actual (p. ej. un dato que debe ser único ya existe). Si se indica el
 * campo de la petición que lo causa, la respuesta lo incluye en {@code errores} para mostrarlo junto a ese campo.
 */
public class ConflictoException extends RuntimeException {

	private final String campo;

	public ConflictoException(String mensaje) {
		this(mensaje, null);
	}

	public ConflictoException(String mensaje, String campo) {
		super(mensaje);
		this.campo = campo;
	}

	public Optional<String> campo() {
		return Optional.ofNullable(campo);
	}

}
