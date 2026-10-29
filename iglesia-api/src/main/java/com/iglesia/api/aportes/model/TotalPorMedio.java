package com.iglesia.api.aportes.model;

import java.math.BigDecimal;

/** Suma y cantidad de aportes de un medio, calculadas en la base. */
public record TotalPorMedio(MedioAporte medio, BigDecimal total, Long cantidad) {
}
