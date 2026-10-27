package com.iglesia.api.common.seguridad.component;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;

/**
 * Autoridades del JWT de iglesia-seguridad: {@code roles} como {@code ROLE_<rol>} (rol base, para el ámbito) y
 * {@code permisos} tal cual, sin prefijo. Los endpoints autorizan siempre por permiso
 * ({@code hasAuthority('miembros.ver')}), nunca por rol.
 */
@Component
public class ConvertidorAutoridadesJwt implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final String PREFIJO_ROL = "ROLE_";

	private static final String SIN_PREFIJO = "";

	private final JwtAuthenticationConverter delegado = new JwtAuthenticationConverter();

	public ConvertidorAutoridadesJwt() {
		delegado.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(
				autoridadesDesde(ClaimsJwt.ROLES, PREFIJO_ROL), autoridadesDesde(ClaimsJwt.PERMISOS, SIN_PREFIJO)));
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		return delegado.convert(jwt);
	}

	private static JwtGrantedAuthoritiesConverter autoridadesDesde(String claim, String prefijo) {
		JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
		autoridades.setAuthoritiesClaimName(claim);
		autoridades.setAuthorityPrefix(prefijo);
		return autoridades;
	}

}
