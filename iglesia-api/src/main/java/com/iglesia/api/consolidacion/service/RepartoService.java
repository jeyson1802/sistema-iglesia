package com.iglesia.api.consolidacion.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.dto.AsignacionPropuestaResponse;
import com.iglesia.api.consolidacion.dto.AsignacionRequest;
import com.iglesia.api.consolidacion.dto.CargaConsolidadorResponse;
import com.iglesia.api.consolidacion.dto.PropuestaRepartoResponse;
import com.iglesia.api.consolidacion.dto.RepartirRequest;
import com.iglesia.api.consolidacion.dto.RondaResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.ConteoPorMiembro;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.Reparto;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Reparto de los casos de una ronda entre el equipo, balanceado por carga (ver {@link Reparto}), y reasignación de
 * casos abiertos. La vista previa calcula lo mismo que el reparto, sin guardar.
 */
@Service
public class RepartoService {

	private static final Logger LOG = LoggerFactory.getLogger(RepartoService.class);

	static final String NADA_QUE_REPARTIR = "No hay casos abiertos para repartir en esta ronda";

	static final String CASO_AJENO = "Hay casos que no son de esta ronda o ya están cerrados";

	static final String CASO_NO_ENCONTRADO = "Caso no encontrado";

	static final String CASO_CERRADO = "Solo se reasignan casos abiertos";

	private final RondaService rondas;

	private final EquipoConsolidacionService equipo;

	private final CasoConsolidacionRepository casos;

	private final MiembrosParaConsolidacion miembros;

	private final ConsolidacionMapper mapper;

	private final UsuarioActual usuarioActual;

	RepartoService(RondaService rondas, EquipoConsolidacionService equipo, CasoConsolidacionRepository casos,
			MiembrosParaConsolidacion miembros, ConsolidacionMapper mapper, UsuarioActual usuarioActual) {
		this.rondas = rondas;
		this.equipo = equipo;
		this.casos = casos;
		this.miembros = miembros;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public PropuestaRepartoResponse vistaPrevia(Long rondaId, RepartirRequest solicitud) {
		rondas.editable(rondaId);
		List<CasoConsolidacion> porRepartir = porRepartir(rondaId, solicitud.soloSinAsignar());
		Map<Long, Long> carga = cargaSin(equipo.paraRepartir(solicitud.consolidadorIds()), porRepartir);
		return propuesta(porRepartir, carga, Reparto.balanceado(ids(porRepartir), carga));
	}

	/** Guarda el reparto balanceado o, si se envían, las asignaciones exactas (la propuesta ajustada a mano). */
	@Transactional
	public RondaResponse repartir(Long rondaId, RepartirRequest solicitud) {
		rondas.editable(rondaId);
		Map<Long, Long> asignaciones = solicitud.asignaciones() == null || solicitud.asignaciones().isEmpty()
				? balanceado(rondaId, solicitud) : exactas(rondaId, solicitud.asignaciones());
		Map<Long, CasoConsolidacion> porId = casos.findAllById(asignaciones.keySet())
			.stream()
			.collect(Collectors.toMap(CasoConsolidacion::getId, Function.identity()));
		asignaciones.forEach((casoId, consolidadorId) -> porId.get(casoId).asignar(consolidadorId));
		LOG.info("Ronda repartida rondaId={} casos={} por usuarioId={}", rondaId, asignaciones.size(), usuario());
		return rondas.obtener(rondaId);
	}

	/** Reasigna casos abiertos (de rondas no cerradas o individuales) a un miembro activo. */
	@Transactional
	public void reasignar(List<Long> casoIds, Long consolidadorId) {
		equipo.personaActiva(consolidadorId);
		List<CasoConsolidacion> elegidos = casos.findAllById(casoIds);
		if (elegidos.size() != casoIds.stream().distinct().count()) {
			throw new RecursoNoEncontradoException(CASO_NO_ENCONTRADO);
		}
		if (elegidos.stream().anyMatch(caso -> !caso.getEstado().abierto())) {
			throw new ReglaNegocioException(CASO_CERRADO, "casoIds");
		}
		elegidos.forEach(caso -> caso.asignar(consolidadorId));
		LOG.info("Casos reasignados casos={} consolidadorId={} por usuarioId={}", elegidos.size(), consolidadorId,
				usuario());
	}

	private Map<Long, Long> balanceado(Long rondaId, RepartirRequest solicitud) {
		List<CasoConsolidacion> porRepartir = porRepartir(rondaId, solicitud.soloSinAsignar());
		if (porRepartir.isEmpty()) {
			throw new ReglaNegocioException(NADA_QUE_REPARTIR);
		}
		Map<Long, Long> carga = cargaSin(equipo.paraRepartir(solicitud.consolidadorIds()), porRepartir);
		return Reparto.balanceado(ids(porRepartir), carga);
	}

	private Map<Long, Long> exactas(Long rondaId, List<AsignacionRequest> asignaciones) {
		List<Long> abiertos = ids(casos.findByRondaIdAndEstadoNotOrderById(rondaId, EstadoCaso.CERRADO));
		if (!abiertos.containsAll(asignaciones.stream().map(AsignacionRequest::casoId).toList())) {
			throw new ReglaNegocioException(CASO_AJENO, "asignaciones");
		}
		equipo.paraRepartir(asignaciones.stream().map(AsignacionRequest::consolidadorId).distinct().toList());
		Map<Long, Long> resultado = new LinkedHashMap<>();
		asignaciones.forEach(asignacion -> resultado.put(asignacion.casoId(), asignacion.consolidadorId()));
		return resultado;
	}

	private List<CasoConsolidacion> porRepartir(Long rondaId, boolean soloSinAsignar) {
		return casos.findByRondaIdAndEstadoNotOrderById(rondaId, EstadoCaso.CERRADO)
			.stream()
			.filter(caso -> !soloSinAsignar || caso.getConsolidadorId() == null)
			.toList();
	}

	/**
	 * Casos abiertos de cada consolidador, sin contar los que se van a repartir de nuevo (si no, quien ya los tenía
	 * parecería más cargado de lo que quedará). Conserva el orden de los consolidadores elegidos.
	 */
	private Map<Long, Long> cargaSin(List<Long> consolidadores, List<CasoConsolidacion> porRepartir) {
		Map<Long, Long> abiertos = casos.abiertosPorConsolidador(consolidadores)
			.stream()
			.collect(Collectors.toMap(ConteoPorMiembro::miembroId, ConteoPorMiembro::cantidad));
		Map<Long, Long> carga = new LinkedHashMap<>();
		for (Long consolidador : consolidadores) {
			long reasignables = porRepartir.stream()
				.filter(caso -> consolidador.equals(caso.getConsolidadorId()))
				.count();
			carga.put(consolidador, abiertos.getOrDefault(consolidador, 0L) - reasignables);
		}
		return carga;
	}

	private PropuestaRepartoResponse propuesta(List<CasoConsolidacion> porRepartir, Map<Long, Long> carga,
			Map<Long, Long> asignaciones) {
		List<Long> personaIds = new ArrayList<>(porRepartir.stream().map(CasoConsolidacion::getMiembroId).toList());
		personaIds.addAll(carga.keySet());
		Map<Long, PersonaConsolidacion> personas = miembros.porIds(personaIds);
		List<AsignacionPropuestaResponse> filas = porRepartir.stream()
			.map(caso -> new AsignacionPropuestaResponse(caso.getId(), mapper.aBreve(personas.get(caso.getMiembroId())),
					asignaciones.get(caso.getId())))
			.toList();
		Map<Long, Long> nuevos = asignaciones.values()
			.stream()
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		List<CargaConsolidadorResponse> cargas = carga.entrySet()
			.stream()
			.map(entrada -> new CargaConsolidadorResponse(entrada.getKey(),
					personas.get(entrada.getKey()).nombreCompleto(), entrada.getValue(),
					nuevos.getOrDefault(entrada.getKey(), 0L)))
			.toList();
		return new PropuestaRepartoResponse(filas, cargas);
	}

	private static List<Long> ids(List<CasoConsolidacion> lista) {
		return lista.stream().map(CasoConsolidacion::getId).toList();
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
