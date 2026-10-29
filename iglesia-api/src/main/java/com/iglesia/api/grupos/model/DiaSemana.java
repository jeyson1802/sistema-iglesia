package com.iglesia.api.grupos.model;

import java.time.DayOfWeek;

/** Día de la semana en que se reúne un grupo o se celebra un culto. */
public enum DiaSemana {

	LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

	/** Mismo orden que ISO-8601 ({@link DayOfWeek#MONDAY} = 1). */
	public static DiaSemana de(DayOfWeek dia) {
		return values()[dia.getValue() - 1];
	}

}
