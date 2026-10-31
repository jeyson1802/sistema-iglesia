package com.iglesia.seguridad.actividad.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.iglesia.seguridad.rol.model.RolBase;

/** Que un usuario usó el sistema un día: una fila por usuario y día (tabla {@code actividad_usuario}). */
@Entity
@Table(name = "actividad_usuario")
public class ActividadUsuario {

	@EmbeddedId
	private ActividadUsuarioId id;

	@Column(name = "iglesia_id")
	private Long iglesiaId;

	@Enumerated(EnumType.STRING)
	@Column(name = "rol_base", nullable = false, length = 20)
	private RolBase rolBase;

	protected ActividadUsuario() {
	}

	public Long getUsuarioId() {
		return id.getUsuarioId();
	}

	public LocalDate getFecha() {
		return id.getFecha();
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

	public RolBase getRolBase() {
		return rolBase;
	}

}
