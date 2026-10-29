package com.iglesia.api.aportes.service;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.iglesia.api.aportes.constant.LimitesAporte;
import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.model.AlcanceAportes;
import com.iglesia.api.aportes.model.CriterioAportes;
import com.iglesia.api.common.error.exception.ReglaNegocioException;

/**
 * Convierte los filtros de la petición en el criterio de consulta: valida el rango de fechas y aplica el ámbito del
 * token. Lo comparten el listado y el resumen para que los dos vean exactamente los mismos aportes.
 */
@Component
class ConsultaAportes {

	static final String RANGO_INVERTIDO = "La fecha final no puede ser anterior a la inicial";

	static final String RANGO_EXCESIVO = "El rango no puede superar " + LimitesAporte.RANGO_MAX_DIAS + " días";

	private final AmbitoAportes ambito;

	ConsultaAportes(AmbitoAportes ambito) {
		this.ambito = ambito;
	}

	/** Vacío si el usuario no puede leer ningún aporte. */
	Optional<CriterioAportes> criterio(FiltroAportes filtro) {
		if (filtro.hasta().isBefore(filtro.desde())) {
			throw new ReglaNegocioException(RANGO_INVERTIDO, "hasta");
		}
		if (ChronoUnit.DAYS.between(filtro.desde(), filtro.hasta()) >= LimitesAporte.RANGO_MAX_DIAS) {
			throw new ReglaNegocioException(RANGO_EXCESIVO, "hasta");
		}
		AlcanceAportes alcance = ambito.actual();
		if (alcance.vacio()) {
			return Optional.empty();
		}
		return Optional.of(new CriterioAportes(filtro.desde(), filtro.hasta(), filtro.categoriaId(), filtro.medio(),
				alcance.aportanteEfectivo(filtro.miembroId())));
	}

	AlcanceAportes alcance() {
		return ambito.actual();
	}

}
