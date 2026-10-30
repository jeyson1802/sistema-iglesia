package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.service.AsistenciaParaConsolidacion;
import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.AvanceConsolidadorResponse;
import com.iglesia.api.consolidacion.dto.AvanceRondaResponse;
import com.iglesia.api.consolidacion.dto.ConteoResultadoResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.ResultadoContacto;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Indicadores de una ronda: embudo (personas → contactadas → volvieron a asistir), estados, reparto por resultado y
 * avance por consolidador. Lee los casos, los contactos y las asistencias de la ronda en una consulta cada uno.
 */
@Service
public class AvanceRondaService {

	private static final double SEGUNDOS_POR_HORA = 3600.0;

	private final RondaService rondas;

	private final CasoConsolidacionRepository casos;

	private final ContactoConsolidacionRepository contactos;

	private final ResultadoConsolidacionService resultados;

	private final AsistenciaParaConsolidacion asistencia;

	private final MiembrosParaConsolidacion miembros;

	private final ConsolidacionMapper mapper;

	private final Clock reloj;

	AvanceRondaService(RondaService rondas, CasoConsolidacionRepository casos,
			ContactoConsolidacionRepository contactos, ResultadoConsolidacionService resultados,
			AsistenciaParaConsolidacion asistencia, MiembrosParaConsolidacion miembros, ConsolidacionMapper mapper,
			Clock reloj) {
		this.rondas = rondas;
		this.casos = casos;
		this.contactos = contactos;
		this.resultados = resultados;
		this.asistencia = asistencia;
		this.miembros = miembros;
		this.mapper = mapper;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public AvanceRondaResponse avance(Long rondaId) {
		rondas.buscar(rondaId);
		List<CasoConsolidacion> lista = casos.findByRondaIdOrderById(rondaId);
		Map<Long, List<ContactoConsolidacion>> porCaso = contactos.deRonda(rondaId)
			.stream()
			.sorted(Comparator.comparing(ContactoConsolidacion::getFechaHora))
			.collect(Collectors.groupingBy(ContactoConsolidacion::getCasoId));
		Map<Long, Instant> primerSeHablo = new LinkedHashMap<>();
		porCaso.forEach((casoId, propios) -> propios.stream()
			.filter(contacto -> contacto.getResultado() == ResultadoContacto.SE_HABLO)
			.findFirst()
			.ifPresent(contacto -> primerSeHablo.put(casoId, contacto.getFechaHora())));
		return new AvanceRondaResponse(lista.size(), primerSeHablo.size(), contar(lista, EstadoCaso.PENDIENTE),
				contar(lista, EstadoCaso.EN_CONTACTO), contar(lista, EstadoCaso.CERRADO),
				lista.stream().filter(caso -> caso.getEstado() == EstadoCaso.CERRADO && caso.getResultadoId() == null).count(),
				volvieronAAsistir(lista, primerSeHablo), horasHastaPrimerContacto(lista, porCaso),
				porResultado(lista), porConsolidador(lista, primerSeHablo));
	}

	/**
	 * Contactadas que asistieron al menos una vez en los 28 días siguientes al día de su primera conversación (sin
	 * contar ese día).
	 */
	private long volvieronAAsistir(List<CasoConsolidacion> lista, Map<Long, Instant> primerSeHablo) {
		if (primerSeHablo.isEmpty()) {
			return 0;
		}
		Map<Long, LocalDate> diaPorMiembro = new LinkedHashMap<>();
		lista.stream()
			.filter(caso -> primerSeHablo.containsKey(caso.getId()))
			.forEach(caso -> diaPorMiembro.put(caso.getMiembroId(),
					LocalDate.ofInstant(primerSeHablo.get(caso.getId()), reloj.getZone())));
		LocalDate desde = diaPorMiembro.values().stream().min(LocalDate::compareTo).orElseThrow().plusDays(1);
		Map<Long, List<LocalDate>> asistidas = asistencia.fechasDesde(diaPorMiembro.keySet(), desde);
		return diaPorMiembro.entrySet().stream().filter(entrada -> {
			LocalDate inicio = entrada.getValue();
			LocalDate fin = inicio.plusDays(LimitesConsolidacion.DIAS_VOLVIO_A_ASISTIR);
			return asistidas.getOrDefault(entrada.getKey(), List.of())
				.stream()
				.anyMatch(dia -> dia.isAfter(inicio) && !dia.isAfter(fin));
		}).count();
	}

	private static Double horasHastaPrimerContacto(List<CasoConsolidacion> lista,
			Map<Long, List<ContactoConsolidacion>> porCaso) {
		OptionalDouble promedio = lista.stream()
			.filter(caso -> porCaso.containsKey(caso.getId()))
			.mapToDouble(caso -> Duration
				.between(caso.getCreadoEn(), porCaso.get(caso.getId()).getFirst().getFechaHora())
				.toSeconds() / SEGUNDOS_POR_HORA)
			.map(horas -> Math.max(horas, 0))
			.average();
		return promedio.isPresent() ? promedio.getAsDouble() : null;
	}

	/** Resultados usados, en el orden del catálogo, y al final los no concluidos. */
	private List<ConteoResultadoResponse> porResultado(List<CasoConsolidacion> lista) {
		Map<Optional<Long>, Long> conteo = lista.stream()
			.filter(caso -> caso.getEstado() == EstadoCaso.CERRADO)
			.collect(Collectors.groupingBy(caso -> Optional.ofNullable(caso.getResultadoId()), Collectors.counting()));
		List<ConteoResultadoResponse> filas = new ArrayList<>();
		resultados.todosPorId()
			.values()
			.stream()
			.sorted(Comparator.comparing(ResultadoConsolidacion::getOrden).thenComparing(ResultadoConsolidacion::getId))
			.filter(resultado -> conteo.containsKey(Optional.of(resultado.getId())))
			.forEach(resultado -> filas.add(new ConteoResultadoResponse(mapper.aReferencia(resultado),
					conteo.get(Optional.of(resultado.getId())))));
		Optional.ofNullable(conteo.get(Optional.<Long>empty()))
			.ifPresent(noConcluidos -> filas.add(new ConteoResultadoResponse(null, noConcluidos)));
		return filas;
	}

	/** Por consolidador, por nombre; los casos sin asignar, al final. */
	private List<AvanceConsolidadorResponse> porConsolidador(List<CasoConsolidacion> lista,
			Map<Long, Instant> primerSeHablo) {
		Map<Optional<Long>, List<CasoConsolidacion>> grupos = lista.stream()
			.collect(Collectors.groupingBy(caso -> Optional.ofNullable(caso.getConsolidadorId())));
		Map<Long, PersonaConsolidacion> personas = miembros
			.porIds(grupos.keySet().stream().flatMap(Optional::stream).toList());
		Comparator<AvanceConsolidadorResponse> orden = Comparator.comparing(
				(AvanceConsolidadorResponse fila) -> fila.consolidador() == null)
			.thenComparing(fila -> fila.consolidador() == null ? "" : fila.consolidador().nombre(),
					String.CASE_INSENSITIVE_ORDER);
		return grupos.entrySet().stream().map(entrada -> {
			ReferenciaResponse consolidador = entrada.getKey().map(personas::get).map(mapper::aReferencia).orElse(null);
			List<CasoConsolidacion> suyos = entrada.getValue();
			return new AvanceConsolidadorResponse(consolidador, suyos.size(),
					suyos.stream().map(CasoConsolidacion::getId).filter(primerSeHablo::containsKey).count(),
					contar(suyos, EstadoCaso.CERRADO));
		}).sorted(orden).toList();
	}

	private static long contar(List<CasoConsolidacion> lista, EstadoCaso estado) {
		return lista.stream().filter(caso -> caso.getEstado() == estado).count();
	}

}
