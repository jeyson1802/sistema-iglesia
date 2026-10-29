package com.iglesia.api.aportes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Moneda de los aportes de una iglesia. La clave es el propio {@code iglesia_id}, así que no extiende
 * {@code EntidadDeIglesia} (un {@code @Id} no puede ser también {@code @TenantId}): el servicio usa siempre la iglesia
 * del token y la RLS de PostgreSQL impide leer o escribir la de otra.
 */
@Entity
@Table(name = "configuracion_aportes")
public class ConfiguracionAportes {

	@Id
	@Column(name = "iglesia_id")
	private Long iglesiaId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "moneda", nullable = false, length = 3)
	private String moneda;

	protected ConfiguracionAportes() {
	}

	public ConfiguracionAportes(Long iglesiaId) {
		this.iglesiaId = iglesiaId;
	}

	public void cambiarMoneda(String nuevaMoneda) {
		moneda = nuevaMoneda;
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

	public String getMoneda() {
		return moneda;
	}

}
