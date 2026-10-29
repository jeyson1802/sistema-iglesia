package com.iglesia.api.asistencia.model;

import com.iglesia.api.asistencia.dto.ReunionResponse;

/** Reunión que se pidió abrir y si se creó ahora (201) o ya existía (200). */
public record ReunionAbierta(ReunionResponse reunion, boolean creada) {
}
