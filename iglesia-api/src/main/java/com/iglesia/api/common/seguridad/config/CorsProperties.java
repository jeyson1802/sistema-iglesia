package com.iglesia.api.common.seguridad.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Orígenes del frontend autorizados a llamar a este backend. */
@Validated
@ConfigurationProperties("api.cors")
public record CorsProperties(@NotEmpty List<String> origenesPermitidos) {
}
