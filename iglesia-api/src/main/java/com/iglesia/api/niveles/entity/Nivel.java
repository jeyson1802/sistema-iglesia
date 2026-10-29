package com.iglesia.api.niveles.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.niveles.constant.LimitesNivel;

/** Rango configurable de los miembros de una iglesia (p. ej. «Pastor», «Diácono»), con su orden jerárquico. */
@Entity
@Table(name = "nivel")
public class Nivel extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesNivel.NOMBRE_MAX)
	private String nombre;

	@Column(name = "orden", nullable = false)
	private int orden;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Nivel() {
	}

	private Nivel(String nombre, int orden) {
		this.nombre = nombre;
		this.orden = orden;
	}

	public static Nivel nuevo(String nombre, int orden) {
		return new Nivel(nombre, orden);
	}

	public void renombrar(String nuevoNombre) {
		nombre = nuevoNombre;
	}

	public void ordenar(int nuevoOrden) {
		orden = nuevoOrden;
	}

	public void cambiarEstado(boolean nuevoEstado) {
		activo = nuevoEstado;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public int getOrden() {
		return orden;
	}

	public boolean isActivo() {
		return activo;
	}

}
