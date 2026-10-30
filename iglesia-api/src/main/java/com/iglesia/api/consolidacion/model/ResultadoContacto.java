package com.iglesia.api.consolidacion.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Qué pasó en un intento de contacto. Cada resultado solo tiene sentido para algunos medios (no se «encuentra» a nadie
 * por teléfono ni «no contesta» en una visita).
 */
public enum ResultadoContacto {

	/** Se habló con la persona. */
	SE_HABLO(EnumSet.allOf(MedioContacto.class)),

	/** No contestó la llamada o no respondió el mensaje. */
	NO_RESPONDIO(EnumSet.of(MedioContacto.LLAMADA, MedioContacto.WHATSAPP)),

	/** Se la visitó y no estaba. */
	NO_ESTABA(EnumSet.of(MedioContacto.VISITA)),

	/** El teléfono o la dirección no son los suyos. */
	DATO_INCORRECTO(EnumSet.of(MedioContacto.LLAMADA, MedioContacto.WHATSAPP, MedioContacto.VISITA));

	private final Set<MedioContacto> medios;

	ResultadoContacto(Set<MedioContacto> medios) {
		this.medios = medios;
	}

	public boolean aplicaA(MedioContacto medio) {
		return medios.contains(medio);
	}

}
