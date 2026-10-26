package com.iglesia.seguridad.jwt.controller;

import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica las claves públicas (JWKS) con las que iglesia-api valida las firmas de los tokens. */
@RestController
@Tag(name = "Claves públicas", description = "Claves para validar la firma de los tokens que emite este servicio.")
@SecurityRequirements
public class JwksController {

	public static final String RUTA = "/oauth2/jwks";

	private final JWKSet conjuntoClavesPublicas;

	JwksController(JWKSet conjuntoClavesPublicas) {
		this.conjuntoClavesPublicas = conjuntoClavesPublicas;
	}

	@GetMapping(RUTA)
	@Operation(summary = "Obtener las claves públicas (JWKS)", description = """
			Conjunto de claves públicas RSA (RFC 7517) con las que se validan los JWT RS256 emitidos por este \
			servicio. Es público: lo consumen iglesia-api y cualquier servicio que valide tokens. Nunca incluye \
			la clave privada. El `kid` (huella de la clave) coincide con la cabecera `kid` del JWT.""")
	@ApiResponse(responseCode = "200", description = "Conjunto de claves públicas.",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject("""
					{"keys": [{"kty": "RSA", "e": "AQAB", "use": "sig", "alg": "RS256",
					  "kid": "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs", "n": "0vx7agoebGcQSuu..."}]}""")))
	Map<String, Object> claves() {
		return conjuntoClavesPublicas.toJSONObject();
	}

}
