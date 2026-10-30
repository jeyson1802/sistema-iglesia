package com.iglesia.api.miembros.model;

import java.time.LocalDate;

/**
 * Persona tal como la necesita la consolidación para listarla y contactarla, sin exponer la entidad ni el resto de su
 * ficha.
 *
 * @param fechaIngreso su fecha de ingreso o, si no la tiene, la de su registro
 * @param grupoId grupo al que pertenece, o {@code null}
 */
public record PersonaConsolidacion(Long id, String nombreCompleto, String telefono, CondicionMiembro condicion,
		LocalDate fechaIngreso, Long grupoId, boolean activo) {
}
