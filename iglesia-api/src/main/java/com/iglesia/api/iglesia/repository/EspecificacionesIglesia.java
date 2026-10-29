package com.iglesia.api.iglesia.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.iglesia.entity.Iglesia;

/**
 * Criterios combinables del catálogo de iglesias. Cada uno devuelve {@link Specification#unrestricted()} si no recibe
 * valor, para combinarlos con {@link Specification#allOf}. Los valores viajan siempre como parámetros enlazados.
 */
public final class EspecificacionesIglesia {

	private static final String NOMBRE_COMERCIAL = "nombreComercial";

	private static final String DENOMINACION = "denominacion";

	private static final String DISTRITO = "distritoCodigo";

	private static final String ACTIVA = "activa";

	private EspecificacionesIglesia() {
	}

	/** Nombre comercial o denominación que contienen el texto, sin distinguir mayúsculas ni tildes. */
	public static Specification<Iglesia> nombreContiene(String texto) {
		if (!StringUtils.hasText(texto)) {
			return Specification.unrestricted();
		}
		String patron = Textos.patronContiene(texto);
		return (iglesia, consulta, criterios) -> criterios.or(
				criterios.like(comparable(criterios, iglesia.get(NOMBRE_COMERCIAL)), patron, Textos.escape()),
				criterios.like(comparable(criterios, iglesia.get(DENOMINACION)), patron, Textos.escape()));
	}

	/**
	 * En el departamento, la provincia o el distrito del código (2, 4 o 6 dígitos): los códigos INEI son jerárquicos,
	 * así que basta con el prefijo del distrito. El código ya viene validado como solo dígitos.
	 */
	public static Specification<Iglesia> enUbigeo(String codigo) {
		if (!StringUtils.hasText(codigo)) {
			return Specification.unrestricted();
		}
		return (iglesia, consulta, criterios) -> criterios.like(iglesia.get(DISTRITO), codigo + "%");
	}

	public static Specification<Iglesia> conEstado(Boolean activa) {
		if (activa == null) {
			return Specification.unrestricted();
		}
		return (iglesia, consulta, criterios) -> criterios.equal(iglesia.get(ACTIVA), activa);
	}

	/** {@code lower(translate(texto, CON_TILDE, SIN_TILDE))}: la misma normalización que {@link Textos#patronContiene}. */
	private static Expression<String> comparable(CriteriaBuilder criterios, Expression<String> texto) {
		return criterios.lower(criterios.function("translate", String.class, texto,
				criterios.literal(Textos.CON_TILDE), criterios.literal(Textos.SIN_TILDE)));
	}

}
