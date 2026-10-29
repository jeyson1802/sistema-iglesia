package com.iglesia.api.asistencia.model;

import java.time.LocalTime;

import com.iglesia.api.grupos.model.DiaSemana;

/** Datos ya validados y normalizados de un horario de culto. */
public record DatosCulto(String nombre, DiaSemana diaSemana, LocalTime hora) {
}
