package com.iglesia.api.miembros.service;

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
import com.iglesia.api.miembros.dto.EstadisticasMiembrosResponse;
import com.iglesia.api.miembros.mapper.EstadisticasMiembrosMapper;
import com.iglesia.api.miembros.repository.EstadisticasMiembrosRepository.FilaResumen;
import com.iglesia.api.miembros.repository.EstadisticasMiembrosRepository;

/** Estadísticas de miembros de la página de inicio: de toda la iglesia o de la red del líder. */
@Service
public class EstadisticasMiembrosService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasMiembrosService.class);

	private final EstadisticasMiembrosRepository consultas;

	private final EstadisticasMiembrosMapper mapper;

	private final ResolutorAlcanceEstadisticas alcances;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasMiembrosService(EstadisticasMiembrosRepository consultas, EstadisticasMiembrosMapper mapper,
			ResolutorAlcanceEstadisticas alcances, CalculadoraPeriodo calculadora, IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.mapper = mapper;
		this.alcances = alcances;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasMiembrosResponse calcular(ConsultaPeriodo periodo) {
		AlcanceEstadisticas alcance = alcances.actual();
		RangoPeriodo rango = calculadora.rango(periodo);
		long iglesiaId = iglesiaActual.requerida();
		Long liderId = alcance.liderId();
		LOG.debug("Estadísticas de miembros periodo={} red={}", periodo, alcance.esRed());
		FilaResumen resumen = consultas.resumen(iglesiaId, liderId, rango.desde(), rango.hasta(), rango.anteriorDesde(),
				rango.anteriorHasta());
		return new EstadisticasMiembrosResponse(PeriodoResponse.de(rango), alcance.esRed(), resumen.getActivos(),
				alcance.esRed() ? resumen.getDirectos() : null, resumen.getAltas(), resumen.getAltasAnterior(),
				resumen.getBajas(), resumen.getBajasAnterior(), resumen.getSinGrupo(),
				alcance.esRed() ? null : resumen.getSinLider(),
				mapper.serie(rango, consultas.serie(iglesiaId, liderId, rango.desde(), rango.hasta(),
						rango.granularidad().unidadSql())),
				mapper.condiciones(consultas.porCondicion(iglesiaId, liderId)),
				mapper.niveles(consultas.porNivel(iglesiaId, liderId)),
				mapper.generos(consultas.porGenero(iglesiaId, liderId)),
				mapper.edades(consultas.porEdad(iglesiaId, liderId, rango.hasta())));
	}

}
