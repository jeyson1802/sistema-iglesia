package com.iglesia.api.aportes.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.aportes.dto.EstadisticasAportesResponse;
import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.dto.PuntoAportesResponse;
import com.iglesia.api.aportes.repository.EstadisticasAportesRepository.FilaSerie;
import com.iglesia.api.aportes.repository.EstadisticasAportesRepository;
import com.iglesia.api.common.estadisticas.component.ResolutorAlcanceEstadisticas;
import com.iglesia.api.common.estadisticas.util.Series;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;
import com.iglesia.api.common.periodo.model.RangoPeriodo;

/**
 * Estadísticas de aportes de la página de inicio de administración: siempre de toda la iglesia. Un líder, aunque
 * tenga {@code aportes.ver} para sus propios aportes, no las obtiene (403): ningún inicio le muestra aportes.
 */
@Service
public class EstadisticasAportesService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasAportesService.class);

	private final ResumenAportesService resumenes;

	private final EstadisticasAportesRepository consultas;

	private final ResolutorAlcanceEstadisticas alcances;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasAportesService(ResumenAportesService resumenes, EstadisticasAportesRepository consultas,
			ResolutorAlcanceEstadisticas alcances, CalculadoraPeriodo calculadora, IglesiaActual iglesiaActual) {
		this.resumenes = resumenes;
		this.consultas = consultas;
		this.alcances = alcances;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasAportesResponse calcular(ConsultaPeriodo periodo) {
		alcances.exigirIglesiaCompleta();
		RangoPeriodo rango = calculadora.rango(periodo);
		LOG.debug("Estadísticas de aportes periodo={}", periodo);
		var resumen = resumenes.resumen(new FiltroAportes(rango.desde(), rango.hasta(), null, null, null, null));
		var filas = consultas.serie(iglesiaActual.requerida(), rango.desde(), rango.hasta(),
				rango.granularidad().unidadSql());
		return new EstadisticasAportesResponse(PeriodoResponse.de(rango), resumen,
				Series.rellenar(rango, filas, FilaSerie::getInicio,
						(inicio, fila) -> fila == null ? new PuntoAportesResponse(inicio, BigDecimal.ZERO, 0)
								: new PuntoAportesResponse(inicio, fila.getTotal(), fila.getCantidad())));
	}

}
