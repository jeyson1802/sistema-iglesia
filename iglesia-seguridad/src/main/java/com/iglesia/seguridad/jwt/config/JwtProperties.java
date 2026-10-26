package com.iglesia.seguridad.jwt.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de emisión de JWT.
 *
 * @param emisor               valor del claim {@code iss}
 * @param audiencias           valores del claim {@code aud} de los tokens emitidos
 * @param audienciaPropia      audiencia que este servicio exige al validar tokens
 * @param duracionTokenAcceso  vigencia del token de acceso (corta)
 * @param duracionTokenRefresco vigencia del token de refresco
 * @param clavePublica         PEM X.509 de la clave pública RSA
 * @param clavePrivada         PEM PKCS#8 de la clave privada RSA
 */
@Validated
@ConfigurationProperties("seguridad.jwt")
public record JwtProperties(
		@NotBlank String emisor,
		@NotEmpty List<String> audiencias,
		@NotBlank String audienciaPropia,
		@NotNull Duration duracionTokenAcceso,
		@NotNull Duration duracionTokenRefresco,
		@NotNull Resource clavePublica,
		@NotNull Resource clavePrivada) {
}
