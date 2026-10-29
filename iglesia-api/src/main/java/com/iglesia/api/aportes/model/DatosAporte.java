package com.iglesia.api.aportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos de un aporte ya validados y normalizados, sin la categoría (que se pasa como entidad). */
public record DatosAporte(LocalDate fecha, Long miembroId, BigDecimal monto, MedioAporte medio, String numeroOperacion,
		String peticion, String observacion) {
}
