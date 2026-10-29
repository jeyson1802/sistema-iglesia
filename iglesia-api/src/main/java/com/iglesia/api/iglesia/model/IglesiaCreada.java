package com.iglesia.api.iglesia.model;

/**
 * Evento de dominio: se dio de alta una iglesia. Se publica dentro de la transacción del alta para que otros
 * contextos (p. ej. aportes) preparen sus datos iniciales sin que {@code iglesia} dependa de ellos.
 */
public record IglesiaCreada(Long iglesiaId) {
}
