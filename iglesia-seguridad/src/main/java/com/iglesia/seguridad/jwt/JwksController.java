package com.iglesia.seguridad.jwt;

import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica las claves públicas (JWKS) con las que iglesia-api valida las firmas de los tokens. */
@RestController
public class JwksController {

	public static final String RUTA = "/oauth2/jwks";

	private final JWKSet conjuntoClavesPublicas;

	JwksController(JWKSet conjuntoClavesPublicas) {
		this.conjuntoClavesPublicas = conjuntoClavesPublicas;
	}

	@GetMapping(RUTA)
	Map<String, Object> claves() {
		return conjuntoClavesPublicas.toJSONObject();
	}

}
