package com.iglesia.api.miembros.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.AmbitoLectura;
import com.iglesia.api.miembros.model.CondicionMiembro;

/**
 * Criterios combinables del listado de miembros. Cada uno devuelve {@link Specification#unrestricted()} si no recibe
 * valor, para combinarlos con {@link Specification#allOf}. Los valores viajan siempre como parámetros enlazados.
 */
public final class EspecificacionesMiembro {

	private static final String ID = "id";

	private static final String NOMBRES = "nombres";

	private static final String APELLIDOS = "apellidos";

	private static final String CONDICION = "condicion";

	private static final String NIVEL = "nivel";

	private static final String LIDER = "lider";

	private static final String GRUPO_ID = "grupoId";

	private static final String ACTIVO = "activo";

	private static final String ESPACIO = " ";

	private EspecificacionesMiembro() {
	}

	/** Solo los miembros que el usuario puede leer; un ámbito vacío no devuelve filas. */
	public static Specification<Miembro> dentroDe(AmbitoLectura ambito) {
		if (ambito.todaLaIglesia()) {
			return Specification.unrestricted();
		}
		if (ambito.ids().isEmpty()) {
			return (miembro, consulta, criterios) -> criterios.disjunction();
		}
		return (miembro, consulta, criterios) -> miembro.get(ID).in(ambito.ids());
	}

	/** Nombres y apellidos (en cualquier orden) que contienen el texto, sin distinguir mayúsculas ni tildes. */
	public static Specification<Miembro> nombreContiene(String texto) {
		if (!StringUtils.hasText(texto)) {
			return Specification.unrestricted();
		}
		String patron = Textos.patronContiene(texto);
		return (miembro, consulta, criterios) -> {
			var nombres = miembro.<String>get(NOMBRES);
			var apellidos = miembro.<String>get(APELLIDOS);
			Expression<String> nombreApellido = comparable(criterios,
					criterios.concat(criterios.concat(nombres, ESPACIO), apellidos));
			Expression<String> apellidoNombre = comparable(criterios,
					criterios.concat(criterios.concat(apellidos, ESPACIO), nombres));
			return criterios.or(criterios.like(nombreApellido, patron, Textos.escape()),
					criterios.like(apellidoNombre, patron, Textos.escape()));
		};
	}

	/** {@code lower(translate(texto, CON_TILDE, SIN_TILDE))}: la misma normalización que {@link Textos#patronContiene}. */
	private static Expression<String> comparable(CriteriaBuilder criterios, Expression<String> texto) {
		return criterios.lower(criterios.function("translate", String.class, texto,
				criterios.literal(Textos.CON_TILDE), criterios.literal(Textos.SIN_TILDE)));
	}

	public static Specification<Miembro> conCondicion(CondicionMiembro condicion) {
		if (condicion == null) {
			return Specification.unrestricted();
		}
		return (miembro, consulta, criterios) -> criterios.equal(miembro.get(CONDICION), condicion);
	}

	public static Specification<Miembro> deNivel(Long nivelId) {
		if (nivelId == null) {
			return Specification.unrestricted();
		}
		return (miembro, consulta, criterios) -> criterios.equal(miembro.get(NIVEL).get(ID), nivelId);
	}

	public static Specification<Miembro> deGrupo(Long grupoId) {
		if (grupoId == null) {
			return Specification.unrestricted();
		}
		return (miembro, consulta, criterios) -> criterios.equal(miembro.get(GRUPO_ID), grupoId);
	}

	/** Liderados directamente por el miembro indicado. */
	public static Specification<Miembro> deLider(Long liderId) {
		if (liderId == null) {
			return Specification.unrestricted();
		}
		return (miembro, consulta, criterios) -> criterios.equal(miembro.get(LIDER).get(ID), liderId);
	}

	public static Specification<Miembro> conEstado(Boolean activo) {
		if (activo == null) {
			return Specification.unrestricted();
		}
		return (miembro, consulta, criterios) -> criterios.equal(miembro.get(ACTIVO), activo);
	}

}
