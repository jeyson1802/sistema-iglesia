package com.iglesia.api.grupos.model;

import java.time.LocalTime;

/**
 * Grupo tal como lo necesita la asistencia: cuándo se reúne, quién lo lidera y el nombre con que se muestra
 * («Célula de Ana Torres»), sin exponer la entidad.
 *
 * @param nombre «&lt;tipo de grupo&gt; de &lt;líder&gt;», con el tipo que configuró la iglesia
 * @param diaReunion día de reunión, o {@code null} si no lo tiene
 * @param horaReunion hora de reunión, o {@code null}
 */
public record GrupoAgenda(Long id, Long liderId, String nombre, DiaSemana diaReunion, LocalTime horaReunion,
		boolean activo) {
}
