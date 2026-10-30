package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.consolidacion.dto.FiltroRondas;
import com.iglesia.api.consolidacion.dto.GuardarRondaRequest;
import com.iglesia.api.consolidacion.dto.RondaResponse;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.DatosRonda;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.model.ResumenRonda;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.EspecificacionesConsolidacion;
import com.iglesia.api.consolidacion.repository.RondaConsolidacionRepository;

/**
 * Rondas de consolidación: alta, edición, borrado mientras están planificadas y su ciclo planificada → en curso →
 * cerrada. Al cerrar, los casos que siguen abiertos quedan como «No concluido».
 */
@Service
public class RondaService {

	private static final Logger LOG = LoggerFactory.getLogger(RondaService.class);

	static final String NO_ENCONTRADA = "Ronda no encontrada";

	static final String NOMBRE_REPETIDO = "Ya existe una ronda con ese nombre";

	static final String FECHAS_INVALIDAS = "La fecha de fin debe ser igual o posterior a la de inicio";

	static final String RONDA_CERRADA = "La ronda está cerrada: es de solo lectura";

	static final String SOLO_PLANIFICADA = "Solo se puede borrar una ronda planificada";

	static final String TRANSICION_INVALIDA = "La ronda no puede pasar de %s a %s";

	static final String RONDA_VACIA = "Agrega personas antes de iniciar la ronda";

	private static final Sort ORDEN = Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("id"));

	private final RondaConsolidacionRepository rondas;

	private final CasoConsolidacionRepository casos;

	private final ConsolidacionMapper mapper;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	RondaService(RondaConsolidacionRepository rondas, CasoConsolidacionRepository casos, ConsolidacionMapper mapper,
			UsuarioActual usuarioActual, Clock reloj) {
		this.rondas = rondas;
		this.casos = casos;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/** Rondas de la más reciente a la más antigua, con el resumen de su avance (una consulta para toda la página). */
	@Transactional(readOnly = true)
	public Page<RondaResponse> listar(FiltroRondas filtro, Pageable paginacion) {
		Specification<RondaConsolidacion> criterio = Specification.allOf(
				EspecificacionesConsolidacion.rondaConEstado(filtro.estado()),
				EspecificacionesConsolidacion.rondaConNombre(filtro.q()));
		Page<RondaConsolidacion> pagina = rondas.findAll(criterio,
				PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize(), ORDEN));
		Map<Long, ResumenRonda> resumenes = resumenes(pagina.getContent().stream().map(RondaConsolidacion::getId).toList());
		return pagina.map(ronda -> mapper.aRonda(ronda, resumenes.getOrDefault(ronda.getId(), ResumenRonda.vacio(ronda.getId()))));
	}

	@Transactional(readOnly = true)
	public RondaResponse obtener(Long id) {
		return respuesta(buscar(id));
	}

	@Transactional
	public RondaResponse crear(GuardarRondaRequest solicitud) {
		RondaConsolidacion ronda = rondas
			.save(RondaConsolidacion.nueva(datos(solicitud, null), usuario(), Instant.now(reloj)));
		LOG.info("Ronda de consolidación creada rondaId={} por usuarioId={}", ronda.getId(), usuario());
		return respuesta(ronda);
	}

	@Transactional
	public RondaResponse actualizar(Long id, GuardarRondaRequest solicitud) {
		RondaConsolidacion ronda = editable(id);
		ronda.actualizar(datos(solicitud, id));
		LOG.info("Ronda de consolidación actualizada rondaId={} por usuarioId={}", id, usuario());
		return respuesta(ronda);
	}

	@Transactional
	public void borrar(Long id) {
		RondaConsolidacion ronda = buscar(id);
		if (ronda.getEstado() != EstadoRonda.PLANIFICADA) {
			throw new ConflictoException(SOLO_PLANIFICADA);
		}
		int quitados = casos.borrarDeRonda(id);
		rondas.delete(ronda);
		LOG.info("Ronda de consolidación borrada rondaId={} casos={} por usuarioId={}", id, quitados, usuario());
	}

	/** Los consolidadores empiezan a ver sus casos en «Mis asignados». */
	@Transactional
	public RondaResponse iniciar(Long id) {
		RondaConsolidacion ronda = buscar(id);
		verificarTransicion(ronda, EstadoRonda.EN_CURSO);
		if (casos.countByRondaId(id) == 0) {
			throw new ConflictoException(RONDA_VACIA);
		}
		ronda.pasarA(EstadoRonda.EN_CURSO, Instant.now(reloj));
		LOG.info("Ronda de consolidación iniciada rondaId={} por usuarioId={}", id, usuario());
		return respuesta(ronda);
	}

	/** Cierra la ronda y deja como «No concluido» (cerrados sin resultado) los casos que seguían abiertos. */
	@Transactional
	public RondaResponse cerrar(Long id) {
		RondaConsolidacion ronda = buscar(id);
		verificarTransicion(ronda, EstadoRonda.CERRADA);
		Instant ahora = Instant.now(reloj);
		int noConcluidos = casos.cerrarAbiertosDeRonda(id, usuario(), ahora);
		ronda.pasarA(EstadoRonda.CERRADA, ahora);
		LOG.info("Ronda de consolidación cerrada rondaId={} noConcluidos={} por usuarioId={}", id, noConcluidos,
				usuario());
		return respuesta(ronda);
	}

	RondaConsolidacion buscar(Long id) {
		return rondas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
	}

	/** Ronda que todavía admite cambios (no cerrada); cerrada, 409. */
	RondaConsolidacion editable(Long id) {
		RondaConsolidacion ronda = buscar(id);
		if (!ronda.getEstado().editable()) {
			throw new ConflictoException(RONDA_CERRADA);
		}
		return ronda;
	}

	Map<Long, RondaConsolidacion> porIds(List<Long> ids) {
		return rondas.findAllById(ids)
			.stream()
			.collect(Collectors.toMap(RondaConsolidacion::getId, Function.identity()));
	}

	private RondaResponse respuesta(RondaConsolidacion ronda) {
		return mapper.aRonda(ronda, resumenes(List.of(ronda.getId())).getOrDefault(ronda.getId(),
				ResumenRonda.vacio(ronda.getId())));
	}

	private Map<Long, ResumenRonda> resumenes(List<Long> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return casos.resumenDe(ids, LocalDate.now(reloj))
			.stream()
			.collect(Collectors.toMap(ResumenRonda::rondaId, Function.identity()));
	}

	private void verificarTransicion(RondaConsolidacion ronda, EstadoRonda destino) {
		if (!ronda.getEstado().puedePasarA(destino)) {
			throw new ConflictoException(TRANSICION_INVALIDA.formatted(ronda.getEstado(), destino));
		}
	}

	private DatosRonda datos(GuardarRondaRequest solicitud, Long idExcluido) {
		if (solicitud.fechaFin() != null && solicitud.fechaFin().isBefore(solicitud.fechaInicio())) {
			throw new ReglaNegocioException(FECHAS_INVALIDAS, "fechaFin");
		}
		String nombre = Textos.normalizar(solicitud.nombre());
		boolean ocupado = rondas.findAll()
			.stream()
			.anyMatch(otra -> !otra.getId().equals(idExcluido) && Textos.mismoTexto(otra.getNombre(), nombre));
		if (ocupado) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
		return new DatosRonda(nombre, Textos.recortarOpcional(solicitud.descripcion()), solicitud.fechaInicio(),
				solicitud.fechaFin());
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
