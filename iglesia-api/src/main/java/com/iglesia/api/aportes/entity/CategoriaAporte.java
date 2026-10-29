package com.iglesia.api.aportes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.aportes.constant.LimitesAporte;
import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;

/** Concepto configurable de los aportes de una iglesia (p. ej. «Diezmo», «Ofrenda», «Misiones»). */
@Entity
@Table(name = "categoria_aporte")
public class CategoriaAporte extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesAporte.NOMBRE_CATEGORIA_MAX)
	private String nombre;

	@Column(name = "exige_aportante", nullable = false)
	private boolean exigeAportante;

	@Column(name = "orden", nullable = false)
	private int orden;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected CategoriaAporte() {
	}

	private CategoriaAporte(String nombre, boolean exigeAportante, int orden) {
		this.nombre = nombre;
		this.exigeAportante = exigeAportante;
		this.orden = orden;
	}

	public static CategoriaAporte nueva(String nombre, boolean exigeAportante, int orden) {
		return new CategoriaAporte(nombre, exigeAportante, orden);
	}

	public void actualizar(String nuevoNombre, boolean nuevoExigeAportante) {
		nombre = nuevoNombre;
		exigeAportante = nuevoExigeAportante;
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

	public boolean isExigeAportante() {
		return exigeAportante;
	}

	public int getOrden() {
		return orden;
	}

	public boolean isActivo() {
		return activo;
	}

}
