package com.iglesia.seguridad.common.seguridad.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;

import com.iglesia.seguridad.jwt.constant.ClaimsJwt;
import com.iglesia.seguridad.rol.model.RolBase;

/**
 * Datos del usuario que hace la petición, tomados del JWT ya validado. {@code iglesiaId} es la iglesia en la que
 * opera: la suya o, para el administrador del sistema, la iglesia a la que entró.
 */
public record UsuarioAutenticado(Long usuarioId, Long iglesiaId, Long miembroId, Set<RolBase> roles,
		Set<String> permisos) {

	public UsuarioAutenticado {
		roles = Set.copyOf(roles);
		permisos = Set.copyOf(permisos);
	}

	public static UsuarioAutenticado desde(Jwt jwt) {
		Collection<String> roles = jwt.getClaimAsStringList(ClaimsJwt.ROLES);
		Set<RolBase> rolesUsuario = EnumSet.noneOf(RolBase.class);
		if (roles != null) {
			roles.stream().map(RolBase::valueOf).forEach(rolesUsuario::add);
		}
		return new UsuarioAutenticado(Long.valueOf(jwt.getSubject()), comoLong(jwt.getClaim(ClaimsJwt.IGLESIA_ID)),
				comoLong(jwt.getClaim(ClaimsJwt.MIEMBRO_ID)), rolesUsuario,
				comoConjunto(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)));
	}

	public boolean tieneRol(RolBase rol) {
		return roles.contains(rol);
	}

	/**
	 * Administrador del sistema en la vista global: actúa sobre todas las iglesias. Dentro de una iglesia ya no es
	 * global y tiene el mismo ámbito que el administrador de esa iglesia. Es la única regla que da ámbito global.
	 */
	public boolean esGlobal() {
		return tieneRol(RolBase.ADMIN_SISTEMA) && iglesiaId == null;
	}

	/** Administrador del sistema que entró a una iglesia (no pertenece a ninguna, así que la iglesia es la elegida). */
	public boolean actuandoEnIglesia() {
		return tieneRol(RolBase.ADMIN_SISTEMA) && iglesiaId != null;
	}

	private static Set<String> comoConjunto(Collection<String> valores) {
		return valores == null ? Set.of() : Set.copyOf(valores);
	}

	private static Long comoLong(Object valor) {
		return valor instanceof Number numero ? numero.longValue() : null;
	}

}
