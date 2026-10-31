package com.iglesia.api.grupos.service;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.estadisticas.component.ResolutorAlcanceEstadisticas;
import com.iglesia.api.common.estadisticas.model.AlcanceEstadisticas;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;
import com.iglesia.api.common.periodo.model.RangoPeriodo;
import com.iglesia.api.grupos.dto.EstadisticasGruposResponse;
import com.iglesia.api.grupos.dto.GrupoEstadisticaResponse;
import com.iglesia.api.grupos.repository.EstadisticasGruposRepository.FilaCobertura;
import com.iglesia.api.grupos.repository.EstadisticasGruposRepository.FilaGrupo;
import com.iglesia.api.grupos.repository.EstadisticasGruposRepository;

/** Estadísticas de grupos de la página de inicio: de toda la iglesia o de los grupos de la red del líder. */
@Service
public class EstadisticasGruposService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasGruposService.class);

	private static final Comparator<GrupoEstadisticaResponse> POR_ASISTENCIA = Comparator
		.comparing(GrupoEstadisticaResponse::porcentaje, Comparator.nullsLast(Comparator.reverseOrder()))
		.thenComparing(GrupoEstadisticaResponse::nombre);

	private final EstadisticasGruposRepository consultas;

	private final ConfiguracionIglesiaService configuracion;

	private final ResolutorAlcanceEstadisticas alcances;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasGruposService(EstadisticasGruposRepository consultas, ConfiguracionIglesiaService configuracion,
			ResolutorAlcanceEstadisticas alcances, CalculadoraPeriodo calculadora, IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.configuracion = configuracion;
		this.alcances = alcances;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasGruposResponse calcular(ConsultaPeriodo periodo) {
		AlcanceEstadisticas alcance = alcances.actual();
		RangoPeriodo rango = calculadora.rango(periodo);
		long iglesiaId = iglesiaActual.requerida();
		LOG.debug("Estadísticas de grupos periodo={} red={}", periodo, alcance.esRed());
		List<FilaGrupo> actuales = consultas.porGrupo(iglesiaId, alcance.liderId(), rango.desde(), rango.hasta());
		List<FilaGrupo> anteriores = consultas.porGrupo(iglesiaId, alcance.liderId(), rango.anteriorDesde(),
				rango.anteriorHasta());
		FilaCobertura cobertura = consultas.cobertura(iglesiaId, alcance.liderId());
		String tipo = configuracion.obtener().nombreGrupoSingular();
		long integrantes = actuales.stream().mapToLong(FilaGrupo::getIntegrantes).sum();
		return new EstadisticasGruposResponse(PeriodoResponse.de(rango), alcance.esRed(), actuales.size(),
				actuales.isEmpty() ? 0 : Math.round(integrantes * 10.0 / actuales.size()) / 10.0,
				cobertura.getEnGrupo(), cobertura.getActivos(),
				actuales.stream().filter(fila -> fila.getReuniones() == 0).count(),
				actuales.stream().mapToLong(FilaGrupo::getReuniones).sum(),
				anteriores.stream().mapToLong(FilaGrupo::getReuniones).sum(), porcentaje(actuales),
				porcentaje(anteriores),
				actuales.stream().map(fila -> aRespuesta(fila, tipo)).sorted(POR_ASISTENCIA).toList());
	}

	private static GrupoEstadisticaResponse aRespuesta(FilaGrupo fila, String tipo) {
		String nombre = fila.getLiderNombre() == null ? tipo : tipo + " de " + fila.getLiderNombre();
		return new GrupoEstadisticaResponse(fila.getGrupoId(), nombre, fila.getIntegrantes(), fila.getReuniones(),
				porcentaje(fila.getEsperadas(), fila.getAsistidas()));
	}

	private static Integer porcentaje(List<FilaGrupo> filas) {
		return porcentaje(filas.stream().mapToLong(FilaGrupo::getEsperadas).sum(),
				filas.stream().mapToLong(FilaGrupo::getAsistidas).sum());
	}

	static Integer porcentaje(long esperadas, long asistidas) {
		return esperadas == 0 ? null : (int) Math.round(asistidas * 100.0 / esperadas);
	}

}
