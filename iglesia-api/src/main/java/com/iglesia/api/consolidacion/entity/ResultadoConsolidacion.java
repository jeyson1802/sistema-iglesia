package com.iglesia.api.consolidacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;

/** Resultado de una iteración de consolidación («Volverá a asistir»…), configurable por iglesia. */
@Entity
@Table(name = "resultado_consolidacion")
public class ResultadoConsolidacion extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesConsolidacion.NOMBRE_RESULTADO_MAX)
	private String nombre;

	@Column(name = "orden", nullable = false)
	private short orden;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected ResultadoConsolidacion() {
	}

	public static ResultadoConsolidacion nuevo(String nombre, int orden) {
		ResultadoConsolidacion resultado = new ResultadoConsolidacion();
		resultado.nombre = nombre;
		resultado.ordenar(orden);
		return resultado;
	}

	public void renombrar(String nuevoNombre) {
		nombre = nuevoNombre;
	}

	public void ordenar(int nuevoOrden) {
		orden = (short) nuevoOrden;
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
