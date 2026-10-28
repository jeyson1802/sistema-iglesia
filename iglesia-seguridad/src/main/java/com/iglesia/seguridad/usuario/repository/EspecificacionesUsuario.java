package com.iglesia.seguridad.usuario.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.iglesia.seguridad.usuario.entity.Usuario;

/**
 * Criterios combinables para buscar usuarios. Cada uno devuelve {@link Specification#unrestricted()} si no recibe
 * valor, para combinarlos con {@link Specification#allOf} sin armar consultas por concatenación. Los valores
 * viajan siempre como parámetros enlazados.
 */
public final class EspecificacionesUsuario {

	private static final String IGLESIA_ID = "iglesiaId";

	private static final String CORREO = "correo";

	private static final String ACTIVO = "activo";

	private static final String ROLES = "roles";

	private static final String ID = "id";

	private static final char ESCAPE = '\\';

	private EspecificacionesUsuario() {
	}

	/** Usuarios de la iglesia; sin iglesia (administrador del sistema sin filtro), todos. */
	public static Specification<Usuario> deIglesia(Long iglesiaId) {
		if (iglesiaId == null) {
			return Specification.unrestricted();
		}
		return (usuario, consulta, criterios) -> criterios.equal(usuario.get(IGLESIA_ID), iglesiaId);
	}

	/** Correo que contiene el texto, sin distinguir mayúsculas. {@code %} y {@code _} se buscan literalmente. */
	public static Specification<Usuario> correoContiene(String texto) {
		if (!StringUtils.hasText(texto)) {
			return Specification.unrestricted();
		}
		String patron = "%" + escaparComodines(texto.strip().toLowerCase()) + "%";
		return (usuario, consulta, criterios) -> criterios.like(criterios.lower(usuario.get(CORREO)), patron,
				ESCAPE);
	}

	public static Specification<Usuario> conEstado(Boolean activo) {
		if (activo == null) {
			return Specification.unrestricted();
		}
		return (usuario, consulta, criterios) -> criterios.equal(usuario.get(ACTIVO), activo);
	}

	/** Usuarios que tienen el rol asignado. Un usuario tiene cada rol una sola vez, así que no repite filas. */
	public static Specification<Usuario> conRol(Long rolId) {
		if (rolId == null) {
			return Specification.unrestricted();
		}
		return (usuario, consulta, criterios) -> criterios.equal(usuario.join(ROLES).get(ID), rolId);
	}

	private static String escaparComodines(String texto) {
		return texto.replace(String.valueOf(ESCAPE), "" + ESCAPE + ESCAPE)
			.replace("%", ESCAPE + "%")
			.replace("_", ESCAPE + "_");
	}

}
