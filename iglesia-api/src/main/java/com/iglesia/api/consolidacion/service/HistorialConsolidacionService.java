package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.CasoAbiertoResponse;
import com.iglesia.api.consolidacion.dto.ConsolidacionMiembroResponse;
import com.iglesia.api.consolidacion.dto.IteracionResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/** Historia de consolidación de una persona: el resumen para su ficha y todas sus iteraciones con sus contactos. */
@Service
public class HistorialConsolidacionService {

	static final String NO_ENCONTRADO = "Miembro no encontrado";

	private final CasoConsolidacionRepository casos;

	private final ResumenesDeCasos resumenes;

	private final RondaService rondas;

	private final ResultadoConsolidacionService resultados;

	private final AmbitoConsolidacion ambito;

	private final MiembrosParaConsolidacion miembros;

	private final ConsolidacionMapper mapper;

	private final Clock reloj;

	HistorialConsolidacionService(CasoConsolidacionRepository casos, ResumenesDeCasos resumenes, RondaService rondas,
			ResultadoConsolidacionService resultados, AmbitoConsolidacion ambito, MiembrosParaConsolidacion miembros,
			ConsolidacionMapper mapper, Clock reloj) {
		this.casos = casos;
		this.resumenes = resumenes;
		this.rondas = rondas;
		this.resultados = resultados;
		this.ambito = ambito;
		this.miembros = miembros;
		this.mapper = mapper;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public ConsolidacionMiembroResponse resumen(Long miembroId) {
		PersonaConsolidacion persona = visible(miembroId);
		List<CasoConsolidacion> todos = casos.findByMiembroIdOrderByCreadoEnDescIdDesc(miembroId);
		List<ContactoConsolidacion> contactos = resumenes
			.contactosDe(todos.stream().map(CasoConsolidacion::getId).toList())
			.values()
			.stream()
			.flatMap(Collection::stream)
			.toList();
		Map<Long, ResultadoConsolidacion> resultadosPorId = resultados.todosPorId();
		Optional<CasoConsolidacion> ultimoCerrado = todos.stream()
			.filter(caso -> caso.getEstado() == EstadoCaso.CERRADO)
			.findFirst();
		Optional<CasoConsolidacion> abierto = todos.stream().filter(caso -> caso.getEstado().abierto()).findFirst();
		boolean puedeIniciar = abierto.isEmpty() && persona.activo() && ambito.puedeIniciarSeguimiento(miembroId);
		return new ConsolidacionMiembroResponse(todos.size(), mapper.porMedio(contactos),
				ultimoCerrado.map(caso -> mapper.aReferencia(resultadosPorId.get(caso.getResultadoId()))).orElse(null),
				ultimoCerrado.map(CasoConsolidacion::getCerradoEn).orElse(null),
				abierto.map(this::aAbierto).orElse(null), fechasRecientes(contactos), puedeIniciar);
	}

	/** Iteraciones de la más reciente a la más antigua, cada una con sus contactos. */
	@Transactional(readOnly = true)
	public Page<IteracionResponse> historial(Long miembroId, Pageable paginacion) {
		visible(miembroId);
		Page<CasoConsolidacion> pagina = casos.findByMiembroIdOrderByCreadoEnDescIdDesc(miembroId, paginacion);
		return new PageImpl<>(resumenes.iteraciones(pagina.getContent()), pagina.getPageable(),
				pagina.getTotalElements());
	}

	private PersonaConsolidacion visible(Long miembroId) {
		return miembros.porId(miembroId)
			.filter(persona -> ambito.puedeVerPersona(persona.id()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	private CasoAbiertoResponse aAbierto(CasoConsolidacion caso) {
		var ronda = caso.esIndividual() ? null : rondas.buscar(caso.getRondaId());
		var consolidador = Optional.ofNullable(caso.getConsolidadorId()).flatMap(miembros::porId).orElse(null);
		return new CasoAbiertoResponse(caso.getId(), mapper.aReferencia(ronda), mapper.aReferencia(consolidador),
				caso.getEstado());
	}

	/** Días con contacto en las últimas semanas, del más antiguo al más reciente. */
	private List<LocalDate> fechasRecientes(List<ContactoConsolidacion> contactos) {
		LocalDate desde = LocalDate.now(reloj).minus(LimitesConsolidacion.SEMANAS_CONTACTOS_RECIENTES, ChronoUnit.WEEKS);
		return contactos.stream()
			.map(contacto -> LocalDate.ofInstant(contacto.getFechaHora(), reloj.getZone()))
			.filter(dia -> !dia.isBefore(desde))
			.distinct()
			.sorted()
			.toList();
	}

}
