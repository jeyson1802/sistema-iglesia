package com.iglesia.seguridad.actividad.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Clave de {@link ActividadUsuario}: el usuario y el día. */
@Embeddable
public class ActividadUsuarioId implements Serializable {

	@Column(name = "usuario_id", nullable = false)
	private Long usuarioId;

	@Column(name = "fecha", nullable = false)
	private LocalDate fecha;

	protected ActividadUsuarioId() {
	}

	public Long getUsuarioId() {
		return usuarioId;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof ActividadUsuarioId clave && Objects.equals(usuarioId, clave.usuarioId)
				&& Objects.equals(fecha, clave.fecha);
	}

	@Override
	public int hashCode() {
		return Objects.hash(usuarioId, fecha);
	}

}
