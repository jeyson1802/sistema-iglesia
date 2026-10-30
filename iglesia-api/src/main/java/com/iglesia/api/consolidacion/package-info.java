/**
 * Consolidación de nuevos y seguimiento pastoral (SPEC 10): equipo de consolidadores, rondas (lotes), casos de cada
 * persona, contactos (llamadas, mensajes y visitas) y el catálogo de resultados de cada iglesia.
 * <p>
 * Estructura del módulo: {@code controller} → {@code service} → {@code repository}, con DTO {@code record} en
 * {@code dto} y entidades que extienden {@link com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia}. Usa
 * miembros y asistencia solo a través de sus {@code service} y {@code model}.
 */
package com.iglesia.api.consolidacion;
