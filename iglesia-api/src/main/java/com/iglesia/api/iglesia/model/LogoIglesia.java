package com.iglesia.api.iglesia.model;

import java.time.Instant;

/** Logo vigente de una iglesia: la imagen, su tipo MIME y cuándo se subió (para el {@code ETag}). */
public record LogoIglesia(byte[] contenido, String tipo, Instant actualizadoEn) {
}
