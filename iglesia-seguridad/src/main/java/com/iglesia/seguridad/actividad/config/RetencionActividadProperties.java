package com.iglesia.seguridad.actividad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/** Cuántos meses se guarda la actividad diaria antes de borrarla. */
@Validated
@ConfigurationProperties("seguridad.actividad")
public record RetencionActividadProperties(@Min(1) int mesesRetencion) {
}
