package com.iglesia.api.aportes.model;

import java.time.LocalDate;

/** Filtros ya validados y con el ámbito aplicado, comunes al listado y al resumen. */
public record CriterioAportes(LocalDate desde, LocalDate hasta, Long categoriaId, MedioAporte medio, Long miembroId) {

	/** El mismo criterio en otro rango de fechas (p. ej. el período anterior). */
	public CriterioAportes entre(LocalDate nuevoDesde, LocalDate nuevoHasta) {
		return new CriterioAportes(nuevoDesde, nuevoHasta, categoriaId, medio, miembroId);
	}

}
