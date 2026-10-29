package com.iglesia.api.asistencia.model;

/** Cantidad de asistentes de una reunión, calculada en la base con {@code COUNT ... GROUP BY}. */
public record TotalPorReunion(Long reunionId, long total) {
}
