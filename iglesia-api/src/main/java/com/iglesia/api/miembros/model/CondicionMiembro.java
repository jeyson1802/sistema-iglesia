package com.iglesia.api.miembros.model;

/** Situación de la persona en la iglesia: ya consolidada, recién llegada o alguien que vuelve. */
public enum CondicionMiembro {

	CONSOLIDADO("Consolidado"), NUEVO("Nuevo"), RECUPERADO("Recuperado");

	private final String etiqueta;

	CondicionMiembro(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	/** Nombre para mostrar en las estadísticas. */
	public String etiqueta() {
		return etiqueta;
	}

}
