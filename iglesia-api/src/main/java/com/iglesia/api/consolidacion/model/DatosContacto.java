package com.iglesia.api.consolidacion.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Datos de un contacto, ya validados.
 *
 * @param consolidadorId miembro que hizo el contacto
 */
public record DatosContacto(Instant fechaHora, MedioContacto medio, ResultadoContacto resultado, String observaciones,
		LocalDate proximoContacto, Long consolidadorId) {
}
