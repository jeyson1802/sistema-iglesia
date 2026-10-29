package com.iglesia.api.ubigeo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Provincia del ubigeo INEI, dentro de un departamento. */
@Entity
@Immutable
@Table(name = "ubigeo_provincia")
public class Provincia {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "codigo", length = 4)
	private String codigo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "departamento_codigo", nullable = false, columnDefinition = "char(2)")
	private Departamento departamento;

	@Column(name = "nombre", nullable = false, length = 60)
	private String nombre;

	protected Provincia() {
	}

	public String getCodigo() {
		return codigo;
	}

	public Departamento getDepartamento() {
		return departamento;
	}

	public String getNombre() {
		return nombre;
	}

}
