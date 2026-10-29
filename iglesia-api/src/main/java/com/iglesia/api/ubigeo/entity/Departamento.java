package com.iglesia.api.ubigeo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Departamento del ubigeo INEI. Catálogo común a todas las iglesias y de solo lectura. */
@Entity
@Immutable
@Table(name = "ubigeo_departamento")
public class Departamento {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "codigo", length = 2)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 60)
	private String nombre;

	protected Departamento() {
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

}
