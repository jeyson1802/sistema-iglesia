package com.iglesia.api.common.multiiglesia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;

/**
 * Base de toda entidad de negocio. Hibernate llena {@code iglesia_id} al insertar y lo agrega como
 * filtro en todas las consultas; nunca se asigna a mano ni se expone en los DTO de entrada.
 */
@MappedSuperclass
public abstract class EntidadDeIglesia {

	@TenantId
	@Column(name = "iglesia_id", nullable = false, updatable = false)
	private Long iglesiaId;

	public Long getIglesiaId() {
		return iglesiaId;
	}

}
