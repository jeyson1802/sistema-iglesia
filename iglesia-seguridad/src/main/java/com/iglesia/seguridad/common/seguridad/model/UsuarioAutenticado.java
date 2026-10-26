package com.iglesia.seguridad.common.seguridad.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;

import com.iglesia.seguridad.jwt.constant.ClaimsJwt;
import com.iglesia.seguridad.usuario.model.Rol;

/** Datos del usuario que hace la petición, tomados del JWT ya validado. */
public record UsuarioAutenticado(Long usuarioId, Long iglesiaId, Long miembroId, Set<Rol> roles) {

	public UsuarioAutenticado {
		roles = Set.copyOf(roles);
	}

	public static UsuarioAutenticado desde(Jwt jwt) {
		Collection<String> roles = jwt.getClaimAsStringList(ClaimsJwt.ROLES);
		Set<Rol> rolesUsuario = EnumSet.noneOf(Rol.class);
		if (roles != null) {
			roles.stream().map(Rol::valueOf).forEach(rolesUsuario::add);
		}
		return new UsuarioAutenticado(Long.valueOf(jwt.getSubject()), comoLong(jwt.getClaim(ClaimsJwt.IGLESIA_ID)),
				comoLong(jwt.getClaim(ClaimsJwt.MIEMBRO_ID)), rolesUsuario);
	}

	public boolean tieneRol(Rol rol) {
		return roles.contains(rol);
	}

	private static Long comoLong(Object valor) {
		return valor instanceof Number numero ? numero.longValue() : null;
	}

}
