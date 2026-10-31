package com.iglesia.api.consolidacion.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.estadisticas.component.ResolutorAlcanceEstadisticas;
import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;
import com.iglesia.api.common.periodo.model.RangoPeriodo;
import com.iglesia.api.consolidacion.dto.EstadisticasConsolidacionResponse;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.repository.EstadisticasConsolidacionRepository.FilaConteo;
import com.iglesia.api.consolidacion.repository.EstadisticasConsolidacionRepository.FilaResumen;
import com.iglesia.api.consolidacion.repository.EstadisticasConsolidacionRepository.FilaRetorno;
import com.iglesia.api.consolidacion.repository.EstadisticasConsolidacionRepository;

/** Estadísticas de consolidación de la página de inicio de administración: siempre de toda la iglesia. */
@Service
public class EstadisticasConsolidacionService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasConsolidacionService.class);

	private static final Map<MedioContacto, String> ETIQUETAS_MEDIO = Map.of(MedioContacto.LLAMADA, "Llamada",
			MedioContacto.WHATSAPP, "WhatsApp", MedioContacto.VISITA, "Visita", MedioContacto.EN_LA_IGLESIA,
			"En la iglesia");

	private final EstadisticasConsolidacionRepository consultas;

	private final ResolutorAlcanceEstadisticas alcances;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasConsolidacionService(EstadisticasConsolidacionRepository consultas,
			ResolutorAlcanceEstadisticas alcances, CalculadoraPeriodo calculadora, IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.alcances = alcances;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasConsolidacionResponse calcular(ConsultaPeriodo periodo) {
		alcances.exigirIglesiaCompleta();
		RangoPeriodo rango = calculadora.rango(periodo);
		long iglesiaId = iglesiaActual.requerida();
		LOG.debug("Estadísticas de consolidación periodo={}", periodo);
		FilaResumen resumen = consultas.resumen(iglesiaId, rango.desde(), rango.hasta(), rango.anteriorDesde(),
				rango.anteriorHasta());
		FilaRetorno retorno = consultas.retorno(iglesiaId, rango.desde(), rango.hasta());
		return new EstadisticasConsolidacionResponse(PeriodoResponse.de(rango), resumen.getRondasEnCurso(),
				resumen.getCasosAbiertos(), resumen.getCerrados(), resumen.getCerradosAnterior(), resumen.getContactos(),
				resumen.getContactosAnterior(), retorno.getContactadas(), retorno.getVolvieron(),
				porMedio(consultas.contactosPorMedio(iglesiaId, rango.desde(), rango.hasta())),
				consultas.cierresPorResultado(iglesiaId, rango.desde(), rango.hasta())
					.stream()
					.map(fila -> new ConteoResponse(fila.getClave(), fila.getClave(), fila.getValor()))
					.toList());
	}

	/** Todos los medios en su orden fijo, con cero si no hubo contactos por ese medio. */
	private static List<ConteoResponse> porMedio(List<FilaConteo> filas) {
		Map<String, Long> valores = filas.stream().collect(Collectors.toMap(FilaConteo::getClave, FilaConteo::getValor));
		return Arrays.stream(MedioContacto.values())
			.map(medio -> new ConteoResponse(medio.name(), ETIQUETAS_MEDIO.get(medio),
					valores.getOrDefault(medio.name(), 0L)))
			.toList();
	}

}
