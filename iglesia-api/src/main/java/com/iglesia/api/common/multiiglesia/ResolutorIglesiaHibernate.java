package com.iglesia.api.common.multiiglesia;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Primera barrera de aislamiento: Hibernate filtra por {@code iglesia_id} todas las entidades con
 * {@code @TenantId} y lo asigna al insertar.
 */
@Component
class ResolutorIglesiaHibernate implements CurrentTenantIdentifierResolver<Long> {

	/**
	 * Valor usado cuando no hay iglesia en el contexto (arranque, tareas internas, administrador del
	 * sistema). No corresponde a ninguna iglesia, así que no devuelve filas y no permite insertar.
	 */
	static final Long SIN_IGLESIA = -1L;

	private final IglesiaActual iglesiaActual;

	ResolutorIglesiaHibernate(IglesiaActual iglesiaActual) {
		this.iglesiaActual = iglesiaActual;
	}

	@Override
	public Long resolveCurrentTenantIdentifier() {
		return iglesiaActual.id().orElse(SIN_IGLESIA);
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

}
