package com.iglesia.api.consolidacion.model;

/**
 * Estado de una ronda: se arma y reparte {@code PLANIFICADA}, los consolidadores trabajan {@code EN_CURSO} y queda de
 * solo lectura {@code CERRADA}. Solo se avanza: planificada → en curso → cerrada.
 */
public enum EstadoRonda {

	PLANIFICADA, EN_CURSO, CERRADA;

	public boolean puedePasarA(EstadoRonda destino) {
		return destino.ordinal() == ordinal() + 1;
	}

	/** Se pueden agregar o quitar personas y repartir: mientras no esté cerrada. */
	public boolean editable() {
		return this != CERRADA;
	}

	/** Sus casos aparecen en «Mis asignados» y admiten contactos. */
	public boolean enCurso() {
		return this == EN_CURSO;
	}

}
