package com.iglesia.api.miembros.model;

/** Miembro tal como lo necesita el contexto de grupos: sin exponer la entidad fuera del registro de miembros. */
public record IntegranteGrupo(Long id, String nombreCompleto, String nivel, Long grupoId) {
}
