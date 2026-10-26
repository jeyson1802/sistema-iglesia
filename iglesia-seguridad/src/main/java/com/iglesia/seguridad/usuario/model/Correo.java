package com.iglesia.seguridad.usuario.model;

import java.util.Locale;

/** Normalización única de correos para que el login y la unicidad no dependan de mayúsculas o espacios. */
public final class Correo {

	private Correo() {
	}

	public static String normalizar(String correo) {
		return correo.strip().toLowerCase(Locale.ROOT);
	}

}
