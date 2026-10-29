package com.iglesia.api.asistencia.model;

import com.iglesia.api.asistencia.dto.MarcaResponse;

/** Resultado de marcar: la respuesta y si se marcó ahora (201) o ya estaba (200). */
public record Marca(MarcaResponse respuesta, boolean nueva) {
}
