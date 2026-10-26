package com.iglesia.seguridad.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.function.Function;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/** Claves RSA, codificador/decodificador de JWT y conversión de claims a autoridades. */
@Configuration(proxyBeanMethods = false)
class JwtConfig {

	private static final String PREFIJO_ROL = "ROLE_";

	@Bean
	RSAKey claveRsa(JwtProperties propiedades) {
		RSAPublicKey publica = leer(propiedades.clavePublica(), RsaKeyConverters.x509()::convert);
		RSAPrivateKey privada = leer(propiedades.clavePrivada(), RsaKeyConverters.pkcs8()::convert);
		try {
			RSAKey sinIdentificador = new RSAKey.Builder(publica).privateKey(privada).build();
			// El kid es la huella de la clave pública: cambia sola al rotar las claves.
			return new RSAKey.Builder(sinIdentificador)
				.keyID(sinIdentificador.computeThumbprint().toString())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256)
				.build();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("No se pudo calcular el identificador de la clave RSA", ex);
		}
	}

	@Bean
	JWKSet conjuntoClavesPublicas(RSAKey claveRsa) {
		return new JWKSet(claveRsa.toPublicJWK());
	}

	@Bean
	JwtEncoder codificadorJwt(RSAKey claveRsa) {
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(claveRsa)));
	}

	@Bean
	JwtDecoder decodificadorJwt(RSAKey claveRsa, JwtProperties propiedades) throws JOSEException {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey(claveRsa.toRSAPublicKey())
			.signatureAlgorithm(SignatureAlgorithm.RS256)
			.build();
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(propiedades.emisor()),
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						audiencias -> audiencias != null && audiencias.contains(propiedades.audienciaPropia()))));
		return decodificador;
	}

	@Bean
	JwtAuthenticationConverter convertidorAutenticacionJwt() {
		JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
		autoridades.setAuthoritiesClaimName(ClaimsJwt.ROLES);
		autoridades.setAuthorityPrefix(PREFIJO_ROL);
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
		return convertidor;
	}

	private static <T> T leer(Resource recurso, Function<InputStream, T> convertidor) {
		try (InputStream entrada = recurso.getInputStream()) {
			return convertidor.apply(entrada);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo leer la clave RSA en " + recurso.getDescription(), ex);
		}
	}

}
