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

/** Distrito del ubigeo INEI, dentro de una provincia. Es lo único que guardan miembros y grupos. */
@Entity
@Immutable
@Table(name = "ubigeo_distrito")
public class Distrito {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "codigo", length = 6)
	private String codigo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "provincia_codigo", nullable = false, columnDefinition = "char(4)")
	private Provincia provincia;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	protected Distrito() {
	}

	public String getCodigo() {
		return codigo;
	}

	public Provincia getProvincia() {
		return provincia;
	}

	public String getNombre() {
		return nombre;
	}

}
