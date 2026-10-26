package com.iglesia.seguridad.auth;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de la cookie que transporta el token de refresco.
 *
 * @param nombre nombre de la cookie
 * @param ruta   atributo {@code Path}: limita la cookie a los endpoints de autenticación
 */
@Validated
@ConfigurationProperties("seguridad.cookie-refresco")
public record CookieRefrescoProperties(@NotBlank String nombre, @NotBlank String ruta) {
}
