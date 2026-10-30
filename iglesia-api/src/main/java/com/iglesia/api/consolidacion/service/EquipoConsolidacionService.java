package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.ConsolidadorResponse;
import com.iglesia.api.consolidacion.entity.Consolidador;
import com.iglesia.api.consolidacion.model.ConteoPorMiembro;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.ConsolidadorRepository;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/** Equipo de consolidación: quiénes reciben casos al repartir una ronda, con su carga actual. */
@Service
public class EquipoConsolidacionService {

	private static final Logger LOG = LoggerFactory.getLogger(EquipoConsolidacionService.class);

	static final String YA_EN_EQUIPO = "Ya forma parte del equipo de consolidación";

	static final String NO_ES_DEL_EQUIPO = "La persona no forma parte del equipo de consolidación";

	static final String MIEMBRO_NO_ENCONTRADO = "Miembro no encontrado";

	static final String MIEMBRO_INACTIVO = "La persona está inactiva";

	static final String SIN_CONSOLIDADORES = "No hay consolidadores activos en el equipo: agrega al menos uno";

	static final String CONSOLIDADOR_NO_VALIDO = "Elige consolidadores activos del equipo";

	private static final String CAMPO_MIEMBRO = "miembroId";

	private static final String CAMPO_CONSOLIDADORES = "consolidadorIds";

	private final ConsolidadorRepository equipo;

	private final CasoConsolidacionRepository casos;

	private final ContactoConsolidacionRepository contactos;

	private final MiembrosParaConsolidacion miembros;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	EquipoConsolidacionService(ConsolidadorRepository equipo, CasoConsolidacionRepository casos,
			ContactoConsolidacionRepository contactos, MiembrosParaConsolidacion miembros, UsuarioActual usuarioActual,
			Clock reloj) {
		this.equipo = equipo;
		this.casos = casos;
		this.contactos = contactos;
		this.miembros = miembros;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/** Activos primero y, dentro de cada grupo, por nombre. */
	@Transactional(readOnly = true)
	public List<ConsolidadorResponse> listar() {
		List<Consolidador> todos = equipo.findAll();
		List<Long> ids = todos.stream().map(Consolidador::getMiembroId).toList();
		Map<Long, PersonaConsolidacion> personas = miembros.porIds(ids);
		Map<Long, Long> abiertos = porMiembro(ids.isEmpty() ? List.of() : casos.abiertosPorConsolidador(ids));
		Instant desde = Instant.now(reloj).minus(Duration.ofDays(LimitesConsolidacion.DIAS_ACTIVIDAD_RECIENTE));
		Map<Long, Long> recientes = porMiembro(
				ids.isEmpty() ? List.of() : contactos.recientesPorConsolidador(ids, desde));
		return todos.stream()
			.map(consolidador -> new ConsolidadorResponse(consolidador.getMiembroId(),
					nombre(personas.get(consolidador.getMiembroId())), consolidador.isActivo(),
					abiertos.getOrDefault(consolidador.getMiembroId(), 0L),
					recientes.getOrDefault(consolidador.getMiembroId(), 0L)))
			.sorted(Comparator.comparing(ConsolidadorResponse::activo)
				.reversed()
				.thenComparing(ConsolidadorResponse::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	/** Agrega a un miembro activo; si estuvo antes en el equipo, lo reactiva. */
	@Transactional
	public ConsolidadorResponse agregar(Long miembroId) {
		PersonaConsolidacion persona = personaActiva(miembroId);
		Consolidador consolidador = equipo.findByMiembroId(miembroId).map(existente -> {
			if (existente.isActivo()) {
				throw new ConflictoException(YA_EN_EQUIPO, CAMPO_MIEMBRO);
			}
			existente.cambiarEstado(true);
			return existente;
		}).orElseGet(() -> equipo.save(Consolidador.nuevo(miembroId, Instant.now(reloj))));
		LOG.info("Consolidador agregado miembroId={} por usuarioId={}", consolidador.getMiembroId(), usuario());
		return new ConsolidadorResponse(miembroId, persona.nombreCompleto(), true, 0, 0);
	}

	@Transactional
	public ConsolidadorResponse cambiarEstado(Long miembroId, boolean activo) {
		Consolidador consolidador = equipo.findByMiembroId(miembroId)
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ES_DEL_EQUIPO));
		consolidador.cambiarEstado(activo);
		LOG.info("Consolidador miembroId={} activo={} por usuarioId={}", miembroId, activo, usuario());
		return listar().stream()
			.filter(respuesta -> respuesta.miembroId().equals(miembroId))
			.findFirst()
			.orElseThrow();
	}

	/**
	 * Consolidadores entre los que se reparte: los elegidos (deben ser activos del equipo) o, sin elegir, todos los
	 * activos. Sin ninguno, 400.
	 */
	List<Long> paraRepartir(List<Long> elegidos) {
		List<Long> activos = equipo.findByActivoTrue().stream().map(Consolidador::getMiembroId).toList();
		if (elegidos == null || elegidos.isEmpty()) {
			if (activos.isEmpty()) {
				throw new ReglaNegocioException(SIN_CONSOLIDADORES, CAMPO_CONSOLIDADORES);
			}
			return activos;
		}
		if (!activos.containsAll(elegidos)) {
			throw new ReglaNegocioException(CONSOLIDADOR_NO_VALIDO, CAMPO_CONSOLIDADORES);
		}
		return elegidos.stream().distinct().toList();
	}

	/** Miembro activo de la iglesia (para el equipo o para consolidar un caso); si no existe 404, inactivo 409. */
	PersonaConsolidacion personaActiva(Long miembroId) {
		PersonaConsolidacion persona = miembros.porId(miembroId)
			.orElseThrow(() -> new RecursoNoEncontradoException(MIEMBRO_NO_ENCONTRADO));
		if (!persona.activo()) {
			throw new ConflictoException(MIEMBRO_INACTIVO, CAMPO_MIEMBRO);
		}
		return persona;
	}

	private static Map<Long, Long> porMiembro(List<ConteoPorMiembro> conteos) {
		return conteos.stream().collect(Collectors.toMap(ConteoPorMiembro::miembroId, ConteoPorMiembro::cantidad));
	}

	private static String nombre(PersonaConsolidacion persona) {
		return persona == null ? "" : persona.nombreCompleto();
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
