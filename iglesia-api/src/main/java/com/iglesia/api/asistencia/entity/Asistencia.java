package com.iglesia.api.asistencia.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import com.iglesia.api.asistencia.model.MedioAsistencia;
import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;

/**
 * Marca de que un miembro asistió a una reunión. Se crea con {@code AsistenciaRepository#marcar} ({@code INSERT ...
 * ON CONFLICT DO NOTHING}), para que dos equipos que leen a la misma persona a la vez no dupliquen ni fallen; esta
 * entidad sirve para leer y quitar marcas.
 */
@Entity
@Table(name = "asistencia")
@IdClass(AsistenciaId.class)
public class Asistencia extends EntidadDeIglesia {

	@Id
	@Column(name = "reunion_id", nullable = false, updatable = false)
	private Long reunionId;

	@Id
	@Column(name = "miembro_id", nullable = false, updatable = false)
	private Long miembroId;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio", nullable = false, length = 10, updatable = false)
	private MedioAsistencia medio;

	@Column(name = "registrada_por", nullable = false, updatable = false)
	private Long registradaPor;

	@Column(name = "registrada_en", nullable = false, updatable = false)
	private Instant registradaEn;

	protected Asistencia() {
	}

	public Long getReunionId() {
		return reunionId;
	}

	public Long getMiembroId() {
		return miembroId;
	}

	public MedioAsistencia getMedio() {
		return medio;
	}

	public Long getRegistradaPor() {
		return registradaPor;
	}

	public Instant getRegistradaEn() {
		return registradaEn;
	}

}
