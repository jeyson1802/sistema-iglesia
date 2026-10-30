package com.iglesia.api.consolidacion.model;

import java.time.LocalDate;

/**
 * Filtro del lote ya traducido a los parámetros de la consulta nativa.
 *
 * @param filtrarIds si solo entran las personas de {@code ids}
 * @param ids ids separados por comas (vacío si no se filtra por ids)
 */
public record CriteriosLote(String condicion, LocalDate ingresoDesde, LocalDate ingresoHasta, Long grupoId,
		boolean sinGrupo, boolean nuncaConsolidados, boolean filtrarIds, String ids) {
}
