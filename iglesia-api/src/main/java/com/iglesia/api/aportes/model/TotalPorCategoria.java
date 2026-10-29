package com.iglesia.api.aportes.model;

import java.math.BigDecimal;

/** Suma y cantidad de aportes de una categoría, calculadas en la base. */
public record TotalPorCategoria(Long categoriaId, BigDecimal total, Long cantidad) {
}
