package com.iglesia.api.asistencia.entity;

import java.io.Serializable;

/**
 * Clave JPA de {@link Asistencia}. En la base la clave primaria también incluye {@code iglesia_id}, pero como el id de
 * la reunión ya es único basta con la reunión y el miembro; la iglesia la aplica Hibernate como tenant.
 */
public record AsistenciaId(Long reunionId, Long miembroId) implements Serializable {
}
