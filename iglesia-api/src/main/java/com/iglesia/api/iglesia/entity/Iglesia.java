package com.iglesia.api.iglesia.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.iglesia.api.iglesia.constant.LimitesIglesia;

/**
 * Iglesia del catálogo de tenants. No extiende {@code EntidadDeIglesia}: es el catálogo mismo, sin
 * {@code iglesia_id} ni RLS, y solo lo administra el administrador del sistema.
 */
@Entity
@Table(name = "iglesia")
public class Iglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesIglesia.NOMBRE_MAX)
	private String nombre;

	@CreationTimestamp
	@Column(name = "creada_en", nullable = false, updatable = false)
	private Instant creadaEn;

	protected Iglesia() {
	}

	private Iglesia(String nombre) {
		this.nombre = nombre;
	}

	public static Iglesia nueva(String nombre) {
		return new Iglesia(nombre);
	}

	public void renombrar(String nuevoNombre) {
		nombre = nuevoNombre;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Instant getCreadaEn() {
		return creadaEn;
	}

}
