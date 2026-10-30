package com.iglesia.api.asistencia.model;

import java.time.LocalDate;

/** Fecha de una reunión a la que asistió un miembro (para medir si volvió después de un contacto). */
public record FechaAsistida(Long miembroId, LocalDate fecha) {
}
