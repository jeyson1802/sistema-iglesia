package com.iglesia.api.miembros.model;

import java.util.Set;

/**
 * Miembros que el usuario del token puede leer: toda la iglesia o solo un conjunto de ids (el líder: él mismo, su red
 * y sus superiores). Un conjunto vacío no deja ver a nadie.
 */
public record AmbitoLectura(boolean todaLaIglesia, Set<Long> ids) {

	public AmbitoLectura {
		ids = Set.copyOf(ids);
	}

	public static AmbitoLectura completo() {
		return new AmbitoLectura(true, Set.of());
	}

	public static AmbitoLectura ninguno() {
		return new AmbitoLectura(false, Set.of());
	}

	public static AmbitoLectura soloDe(Set<Long> ids) {
		return new AmbitoLectura(false, ids);
	}

	public boolean incluye(Long miembroId) {
		return todaLaIglesia || ids.contains(miembroId);
	}

}
