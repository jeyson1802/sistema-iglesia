package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.dto.CasoResumenResponse;
import com.iglesia.api.consolidacion.dto.ContactoResponse;
import com.iglesia.api.consolidacion.dto.IteracionAnteriorResponse;
import com.iglesia.api.consolidacion.dto.IteracionResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Arma las respuestas de varios casos a la vez con un número fijo de consultas (contactos, personas, rondas, casos
 * anteriores y resultados), sin importar cuántos casos haya: así las listas no caen en N+1.
 * <p>
 * La iteración de un caso es su posición entre los casos de la persona, del más antiguo al más reciente (los ids
 * crecen con el tiempo).
 */
@Component
class ResumenesDeCasos {

	private static final Comparator<CasoConsolidacion> MAS_RECIENTE_PRIMERO = Comparator
		.comparing(CasoConsolidacion::getId)
		.reversed();

	private final CasoConsolidacionRepository casos;

	private final ContactoConsolidacionRepository contactos;

	private final RondaService rondas;

	private final ResultadoConsolidacionService resultados;

	private final MiembrosParaConsolidacion miembros;

	private final ConsolidacionMapper mapper;

	private final AmbitoConsolidacion ambito;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	ResumenesDeCasos(CasoConsolidacionRepository casos, ContactoConsolidacionRepository contactos, RondaService rondas,
			ResultadoConsolidacionService resultados, MiembrosParaConsolidacion miembros, ConsolidacionMapper mapper,
			AmbitoConsolidacion ambito, UsuarioActual usuarioActual, Clock reloj) {
		this.casos = casos;
		this.contactos = contactos;
		this.rondas = rondas;
		this.resultados = resultados;
		this.miembros = miembros;
		this.mapper = mapper;
		this.ambito = ambito;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	List<CasoResumenResponse> resumir(List<CasoConsolidacion> lista) {
		if (lista.isEmpty()) {
			return List.of();
		}
		Map<Long, List<ContactoConsolidacion>> contactosPorCaso = contactosDe(ids(lista));
		Map<Long, List<CasoConsolidacion>> historia = casos
			.findByMiembroIdIn(lista.stream().map(CasoConsolidacion::getMiembroId).distinct().toList())
			.stream()
			.collect(Collectors.groupingBy(CasoConsolidacion::getMiembroId));
		Map<Long, PersonaConsolidacion> personas = miembros.porIds(Stream
			.concat(lista.stream().map(CasoConsolidacion::getMiembroId),
					lista.stream().map(CasoConsolidacion::getConsolidadorId))
			.toList());
		Map<Long, RondaConsolidacion> rondasPorId = rondas.porIds(rondaIds(lista));
		Map<Long, ResultadoConsolidacion> resultadosPorId = resultados.todosPorId();
		return lista.stream().map(caso -> {
			List<CasoConsolidacion> anteriores = anteriores(caso, historia.getOrDefault(caso.getMiembroId(), List.of()));
			List<ContactoConsolidacion> propios = contactosPorCaso.getOrDefault(caso.getId(), List.of());
			IteracionAnteriorResponse anterior = anteriores.isEmpty() ? null
					: aAnterior(anteriores.getFirst(), resultadosPorId);
			return new CasoResumenResponse(caso.getId(), mapper.aReferencia(rondasPorId.get(caso.getRondaId())),
					mapper.aPersona(personas.get(caso.getMiembroId())),
					mapper.aReferencia(personas.get(caso.getConsolidadorId())), caso.getEstado(),
					mapper.porMedio(propios), mapper.aUltimo(propios), caso.getProximoContacto(),
					mapper.aReferencia(resultadosPorId.get(caso.getResultadoId())), anteriores.size() + 1, anterior);
		}).toList();
	}

	/** Iteraciones completas (con sus contactos), en el orden recibido. */
	List<IteracionResponse> iteraciones(List<CasoConsolidacion> lista) {
		if (lista.isEmpty()) {
			return List.of();
		}
		Map<Long, List<ContactoConsolidacion>> contactosPorCaso = contactosDe(ids(lista));
		List<Long> personaIds = new ArrayList<>(lista.stream().map(CasoConsolidacion::getConsolidadorId).toList());
		contactosPorCaso.values().forEach(propios -> propios.forEach(c -> personaIds.add(c.getConsolidadorId())));
		Map<Long, PersonaConsolidacion> personas = miembros.porIds(personaIds);
		Map<Long, RondaConsolidacion> rondasPorId = rondas.porIds(rondaIds(lista));
		Map<Long, ResultadoConsolidacion> resultadosPorId = resultados.todosPorId();
		return lista.stream().map(caso -> {
			List<ContactoConsolidacion> propios = contactosPorCaso.getOrDefault(caso.getId(), List.of());
			return new IteracionResponse(caso.getId(), mapper.aReferencia(rondasPorId.get(caso.getRondaId())),
					caso.getEstado(), caso.getCreadoEn(), caso.getCerradoEn(),
					mapper.aReferencia(personas.get(caso.getConsolidadorId())), mapper.porMedio(propios),
					mapper.aReferencia(resultadosPorId.get(caso.getResultadoId())), caso.getObservacionCierre(),
					contactos(propios, personas));
		}).toList();
	}

	List<ContactoResponse> contactos(List<ContactoConsolidacion> lista, Map<Long, PersonaConsolidacion> personas) {
		Long usuarioId = usuarioActual.id().orElse(null);
		boolean coordinador = ambito.esCoordinador();
		Instant ahora = Instant.now(reloj);
		return lista.stream()
			.map(contacto -> mapper.aContacto(contacto, personas, usuarioId, coordinador, ahora))
			.toList();
	}

	ContactoResponse contacto(ContactoConsolidacion contacto) {
		return contactos(List.of(contacto), miembros.porIds(List.of(contacto.getConsolidadorId()))).getFirst();
	}

	IteracionAnteriorResponse aAnterior(CasoConsolidacion caso, Map<Long, ResultadoConsolidacion> resultadosPorId) {
		return new IteracionAnteriorResponse(mapper.aReferencia(resultadosPorId.get(caso.getResultadoId())),
				caso.getCerradoEn());
	}

	/** Casos de la persona anteriores a este, del más reciente al más antiguo. */
	static List<CasoConsolidacion> anteriores(CasoConsolidacion caso, Collection<CasoConsolidacion> todos) {
		return todos.stream().filter(otro -> otro.getId() < caso.getId()).sorted(MAS_RECIENTE_PRIMERO).toList();
	}

	Map<Long, List<ContactoConsolidacion>> contactosDe(List<Long> casoIds) {
		if (casoIds.isEmpty()) {
			return Map.of();
		}
		return contactos.findByCasoIdInOrderByFechaHoraDescIdDesc(casoIds)
			.stream()
			.collect(Collectors.groupingBy(ContactoConsolidacion::getCasoId));
	}

	private static List<Long> ids(List<CasoConsolidacion> lista) {
		return lista.stream().map(CasoConsolidacion::getId).toList();
	}

	private static List<Long> rondaIds(List<CasoConsolidacion> lista) {
		return lista.stream().map(CasoConsolidacion::getRondaId).filter(Objects::nonNull).distinct().toList();
	}

}
