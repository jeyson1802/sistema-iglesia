package com.iglesia.seguridad.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.usuario.Usuario;

/**
 * Emite el token de acceso (JWT RS256). El {@code sub} es el id del usuario; los datos personales
 * no viajan en el token. Incluye roles, iglesia (tenant) y miembro vinculado cuando existen.
 */
@Component
public class GeneradorTokenAcceso {

	private final JwtEncoder codificador;

	private final JwtProperties propiedades;

	private final String identificadorClave;

	private final Clock reloj;

	GeneradorTokenAcceso(JwtEncoder codificador, JwtProperties propiedades, RSAKey claveRsa, Clock reloj) {
		this.codificador = codificador;
		this.propiedades = propiedades;
		this.identificadorClave = claveRsa.getKeyID();
		this.reloj = reloj;
	}

	public TokenAcceso generar(Usuario usuario) {
		Instant ahora = reloj.instant();
		Instant expiraEn = ahora.plus(propiedades.duracionTokenAcceso());
		List<String> roles = usuario.getRoles().stream().map(Enum::name).sorted().toList();

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.id(UUID.randomUUID().toString())
			.issuer(propiedades.emisor())
			.audience(propiedades.audiencias())
			.subject(usuario.getId().toString())
			.issuedAt(ahora)
			.expiresAt(expiraEn)
			.claim(ClaimsJwt.ROLES, roles);
		if (usuario.getIglesiaId() != null) {
			claims.claim(ClaimsJwt.IGLESIA_ID, usuario.getIglesiaId());
		}
		if (usuario.getMiembroId() != null) {
			claims.claim(ClaimsJwt.MIEMBRO_ID, usuario.getMiembroId());
		}

		JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(identificadorClave).build();
		String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
		return new TokenAcceso(valor, expiraEn);
	}

}
