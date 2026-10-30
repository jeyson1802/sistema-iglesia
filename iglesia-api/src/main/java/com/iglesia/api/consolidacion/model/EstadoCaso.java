package com.iglesia.api.consolidacion.model;

/** Estado de un caso: sin contactos, con al menos uno, o cerrado (con o sin resultado). */
public enum EstadoCaso {

	PENDIENTE, EN_CONTACTO, CERRADO;

	public boolean abierto() {
		return this != CERRADO;
	}

}
