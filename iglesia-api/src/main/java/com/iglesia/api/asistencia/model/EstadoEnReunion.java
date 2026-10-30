package com.iglesia.api.asistencia.model;

/** Cómo quedó una persona en una reunión de grupo. */
public enum EstadoEnReunion {

	/** Integrante del grupo que asistió. */
	ASISTIO,

	/** Integrante del grupo al que le correspondía la reunión y no asistió. */
	FALTO,

	/** Asistió sin ser integrante del grupo (p. ej. de otro grupo o sin grupo). */
	VISITANTE

}
