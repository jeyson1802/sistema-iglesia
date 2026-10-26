package com.iglesia.seguridad.rol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Permiso del catálogo fijo, con la forma {@code <modulo>.<accion>}. Es de solo lectura: cada permiso
 * corresponde a código real y solo se agrega por migración de Flyway.
 */
@Entity
@Immutable
@Table(name = "permiso")
public class Permiso {

	@Id
	@Column(length = 60)
	private String codigo;

	@Column(nullable = false, length = 40)
	private String modulo;

	@Column(nullable = false, length = 200)
	private String descripcion;

	@Column(nullable = false)
	private int orden;

	protected Permiso() {
	}

	public String getCodigo() {
		return codigo;
	}

	public String getModulo() {
		return modulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public int getOrden() {
		return orden;
	}

	@Override
	public boolean equals(Object otro) {
		return this == otro || (otro instanceof Permiso permiso && codigo != null && codigo.equals(permiso.getCodigo()));
	}

	@Override
	public int hashCode() {
		return Permiso.class.hashCode();
	}

}
