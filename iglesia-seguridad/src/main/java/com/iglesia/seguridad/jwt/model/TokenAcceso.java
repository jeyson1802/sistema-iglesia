package com.iglesia.seguridad.jwt.model;

import java.time.Instant;

/** Token de acceso firmado y su instante de expiración. */
public record TokenAcceso(String valor, Instant expiraEn) {
}
