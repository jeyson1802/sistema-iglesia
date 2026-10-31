package com.iglesia.api.asistencia.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.EstadisticasAsistenciaResponse;
import com.iglesia.api.asistencia.dto.PuntoCultosResponse;
import com.iglesia.api.asistencia.repository.EstadisticasAsistenciaRepository.FilaCultos;
import com.iglesia.api.asistencia.repository.EstadisticasAsistenciaRepository.FilaRetorno;
import com.iglesia.api.asistencia.repository.EstadisticasAsistenciaRepository;
import com.iglesia.api.common.estadisticas.component.ResolutorAlcanceEstadisticas;
import com.iglesia.api.common.estadisticas.model.AlcanceEstadisticas;
import com.iglesia.api.common.estadisticas.util.Series;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;
import com.iglesia.api.common.periodo.model.RangoPeriodo;

/**
 * Estadísticas de asistencia de la página de inicio: de toda la iglesia o de la red del líder. Las alertas salen del
 * seguimiento de asistencia, con su propio ámbito (toda la iglesia con {@code asistencia.ver}, la red con
 * {@code asistencia.grupo}).
 */
@Service
public class EstadisticasAsistenciaService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasAsistenciaService.class);

	private final EstadisticasAsistenciaRepository consultas;

	private final SeguimientoAsistenciaService seguimiento;

	private final ResolutorAlcanceEstadisticas alcances;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasAsistenciaService(EstadisticasAsistenciaRepository consultas, SeguimientoAsistenciaService seguimiento,
			ResolutorAlcanceEstadisticas alcances, CalculadoraPeriodo calculadora, IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.seguimiento = seguimiento;
		this.alcances = alcances;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasAsistenciaResponse calcular(ConsultaPeriodo periodo) {
		AlcanceEstadisticas alcance = alcances.actual();
		RangoPeriodo rango = calculadora.rango(periodo);
		long iglesiaId = iglesiaActual.requerida();
		Long liderId = alcance.liderId();
		String unidad = rango.granularidad().unidadSql();
		LOG.debug("Estadísticas de asistencia periodo={} red={}", periodo, alcance.esRed());
		List<FilaCultos> actuales = consultas.cultos(iglesiaId, liderId, rango.desde(), rango.hasta(), unidad);
		List<FilaCultos> anteriores = consultas.cultos(iglesiaId, liderId, rango.anteriorDesde(), rango.anteriorHasta(),
				unidad);
		FilaRetorno retorno = consultas.retorno(iglesiaId, liderId, rango.desde(), rango.hasta());
		return new EstadisticasAsistenciaResponse(PeriodoResponse.de(rango), alcance.esRed(), totalCultos(actuales),
				totalCultos(anteriores), promedio(actuales), promedio(anteriores),
				consultas.personasQueAsistieron(iglesiaId, liderId, rango.desde(), rango.hasta()),
				consultas.personasQueAsistieron(iglesiaId, liderId, rango.anteriorDesde(), rango.anteriorHasta()),
				Series.rellenar(rango, actuales, FilaCultos::getInicio,
						(inicio, fila) -> fila == null ? new PuntoCultosResponse(inicio, 0, null)
								: new PuntoCultosResponse(inicio, fila.getCultos(),
										promedio(fila.getAsistentes(), fila.getCultos()))),
				seguimiento.conteos(null, null), retorno.getLlegaron(), retorno.getVolvieron());
	}

	private static long totalCultos(List<FilaCultos> filas) {
		return filas.stream().mapToLong(FilaCultos::getCultos).sum();
	}

	private static Double promedio(List<FilaCultos> filas) {
		return promedio(filas.stream().mapToLong(FilaCultos::getAsistentes).sum(), totalCultos(filas));
	}

	/** Con un decimal; nulo si no hubo cultos. */
	static Double promedio(long asistentes, long cultos) {
		return cultos == 0 ? null : Math.round(asistentes * 10.0 / cultos) / 10.0;
	}

}
