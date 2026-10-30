package com.iglesia.api.consolidacion.model;

import java.time.LocalDate;

/** Datos editables de una ronda, ya validados y normalizados. */
public record DatosRonda(String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin) {
}
