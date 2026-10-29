package com.iglesia.api.common.error.exception;

import java.util.Optional;

/**
 * La petición es válida en formato pero incumple una regla del dominio. Si se indica el
 * campo de la petición que lo causa, la respuesta lo incluye en {@code errores} para mostrarlo junto a ese campo.
 */
public class ReglaNegocioException extends RuntimeException {

	private final String campo;

	public ReglaNegocioException(String mensaje) {
		this(mensaje, null);
	}

	public ReglaNegocioException(String mensaje, String campo) {
		super(mensaje);
		this.campo = campo;
	}

	public Optional<String> campo() {
		return Optional.ofNullable(campo);
	}

}
