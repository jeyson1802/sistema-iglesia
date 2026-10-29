package com.iglesia.api.niveles.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.niveles.dto.GuardarNivelRequest;
import com.iglesia.api.niveles.dto.NivelResponse;
import com.iglesia.api.niveles.dto.OrdenarNivelesRequest;
import com.iglesia.api.niveles.entity.Nivel;
import com.iglesia.api.niveles.mapper.NivelMapper;
import com.iglesia.api.niveles.repository.NivelRepository;

/**
 * Catálogo de niveles de la iglesia del token. El nombre es único sin distinguir mayúsculas ni tildes en mayúscula
 * (el índice {@code ux_nivel_nombre} lo respalda en la base); los nuevos van al final del orden. Un nivel nunca se borra: se desactiva.
 */
@Service
public class NivelService {

	private static final Logger LOG = LoggerFactory.getLogger(NivelService.class);

	static final String NO_ENCONTRADO = "Nivel no encontrado";

	static final String NOMBRE_REPETIDO = "Ya existe un nivel con ese nombre";

	static final String ORDEN_INCOMPLETO = "El orden debe incluir todos los niveles de la iglesia, una sola vez cada uno";

	static final String INEXISTENTE = "El nivel no existe";

	static final String INACTIVO = "El nivel está desactivado y no se puede asignar";

	private final NivelRepository niveles;

	private final NivelMapper mapper;

	private final UsuarioActual usuarioActual;

	NivelService(NivelRepository niveles, NivelMapper mapper, UsuarioActual usuarioActual) {
		this.niveles = niveles;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<NivelResponse> listar() {
		return niveles.findAllByOrderByOrdenAscIdAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional
	public NivelResponse crear(GuardarNivelRequest solicitud) {
		String nombre = Textos.normalizar(solicitud.nombre());
		if (nombreOcupado(nombre, null)) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
		Nivel nivel = guardar(Nivel.nuevo(nombre, niveles.ultimoOrden() + 1));
		LOG.info("Nivel creado id={} por usuarioId={}", nivel.getId(), usuarioActual.id().orElse(null));
		return mapper.aRespuesta(nivel);
	}

	@Transactional
	public NivelResponse renombrar(Long id, GuardarNivelRequest solicitud) {
		Nivel nivel = buscar(id);
		String nombre = Textos.normalizar(solicitud.nombre());
		if (nombreOcupado(nombre, id)) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
		nivel.renombrar(nombre);
		guardar(nivel);
		LOG.info("Nivel renombrado id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return mapper.aRespuesta(nivel);
	}

	/** Reordena todos los niveles: la lista debe traer cada nivel de la iglesia exactamente una vez. */
	@Transactional
	public List<NivelResponse> ordenar(OrdenarNivelesRequest solicitud) {
		List<Nivel> todos = niveles.findAllByOrderByOrdenAscIdAsc();
		Set<Long> existentes = todos.stream().map(Nivel::getId).collect(Collectors.toSet());
		if (solicitud.ids().size() != todos.size() || !existentes.equals(new HashSet<>(solicitud.ids()))) {
			throw new ReglaNegocioException(ORDEN_INCOMPLETO, "ids");
		}
		for (Nivel nivel : todos) {
			nivel.ordenar(solicitud.ids().indexOf(nivel.getId()) + 1);
		}
		LOG.info("Niveles reordenados por usuarioId={}", usuarioActual.id().orElse(null));
		return niveles.findAllByOrderByOrdenAscIdAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional
	public NivelResponse cambiarEstado(Long id, boolean activo) {
		Nivel nivel = buscar(id);
		nivel.cambiarEstado(activo);
		LOG.info("Nivel {} id={} por usuarioId={}", activo ? "reactivado" : "desactivado", id,
				usuarioActual.id().orElse(null));
		return mapper.aRespuesta(nivel);
	}

	/**
	 * Nivel que se asigna a un miembro. Si no cambia respecto del actual se acepta aunque esté inactivo (los ya
	 * asignados se conservan); si cambia, debe existir en la iglesia y estar activo (409).
	 */
	@Transactional(readOnly = true)
	public Nivel asignable(Long nuevoId, Nivel actual) {
		if (nuevoId == null) {
			return null;
		}
		if (actual != null && Objects.equals(actual.getId(), nuevoId)) {
			return actual;
		}
		Nivel nivel = niveles.findById(nuevoId).orElseThrow(() -> new ConflictoException(INEXISTENTE, "nivelId"));
		if (!nivel.isActivo()) {
			throw new ConflictoException(INACTIVO, "nivelId");
		}
		return nivel;
	}

	/** El catálogo es pequeño: se compara en Java para no depender de la configuración regional de la base. */
	private boolean nombreOcupado(String nombre, Long idExcluido) {
		return niveles.findAll()
			.stream()
			.anyMatch(nivel -> !nivel.getId().equals(idExcluido) && Textos.mismoTexto(nivel.getNombre(), nombre));
	}

	private Nivel buscar(Long id) {
		return niveles.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	/** Escribe ya para que un nombre repetido por un alta simultánea responda 409 y no 500. */
	private Nivel guardar(Nivel nivel) {
		try {
			return niveles.saveAndFlush(nivel);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
	}

}
