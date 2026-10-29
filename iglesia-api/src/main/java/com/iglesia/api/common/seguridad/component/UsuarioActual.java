package com.iglesia.api.common.seguridad.component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

/**
 * Usuario de la petición actual, leído del JWT validado: el claim {@code sub} (id del usuario en iglesia-seguridad),
 * el miembro vinculado ({@code miembro_id}) y sus roles base ({@code roles}). Sirve para registrar quién hace cada
 * cambio sin datos personales y para calcular qué filas puede leer.
 */
@Component
public class UsuarioActual {

	public Optional<Long> id() {
		return token().map(jwt -> Long.valueOf(jwt.getSubject()));
	}

	/** Miembro de iglesia-api vinculado al usuario, si lo tiene. */
	public Optional<Long> miembroId() {
		return token().map(jwt -> jwt.getClaim(ClaimsJwt.MIEMBRO_ID))
			.filter(Number.class::isInstance)
			.map(valor -> ((Number) valor).longValue());
	}

	/** Códigos de los roles base activos del usuario ({@code ADMIN_IGLESIA}, {@code LIDER}...). */
	public Set<String> rolesBase() {
		return token().map(jwt -> jwt.getClaim(ClaimsJwt.ROLES))
			.filter(Collection.class::isInstance)
			.map(roles -> Set.copyOf(((Collection<?>) roles).stream().map(String::valueOf).toList()))
			.orElse(Set.of());
	}

	/** Si el token trae el permiso (las autoridades sin prefijo son los permisos del claim {@code permisos}). */
	public boolean tienePermiso(String permiso) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		return autenticacion != null && autenticacion.getAuthorities()
			.stream()
			.anyMatch(autoridad -> permiso.equals(autoridad.getAuthority()));
	}

	private static Optional<Jwt> token() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion instanceof JwtAuthenticationToken token) {
			return Optional.of(token.getToken());
		}
		return Optional.empty();
	}

}
