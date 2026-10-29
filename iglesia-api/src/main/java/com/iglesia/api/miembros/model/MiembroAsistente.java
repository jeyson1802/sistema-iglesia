package com.iglesia.api.miembros.model;

/**
 * Miembro tal como lo necesita el contexto de asistencia: lo que se muestra al marcarlo, sin exponer la entidad ni
 * datos personales.
 *
 * @param nivel nombre de su nivel, o {@code null}
 * @param grupoId grupo al que pertenece, o {@code null}
 */
public record MiembroAsistente(Long id, String nombreCompleto, String nivel, Long grupoId, boolean activo) {
}
