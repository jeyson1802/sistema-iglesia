package com.iglesia.api.consolidacion.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;

/** Miembro del equipo de consolidación: candidato al repartir una ronda. Se desactiva, no se borra. */
@Entity
@Table(name = "consolidador")
public class Consolidador extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "miembro_id", nullable = false, updatable = false)
	private Long miembroId;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Consolidador() {
	}

	public static Consolidador nuevo(Long miembroId, Instant ahora) {
		Consolidador consolidador = new Consolidador();
		consolidador.miembroId = miembroId;
		consolidador.creadoEn = ahora;
		return consolidador;
	}

	public void cambiarEstado(boolean nuevoEstado) {
		activo = nuevoEstado;
	}

	public Long getId() {
		return id;
	}

	public Long getMiembroId() {
		return miembroId;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
