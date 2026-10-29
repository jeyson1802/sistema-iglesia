package com.iglesia.api.iglesia.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.mapper.IglesiaMapper;
import com.iglesia.api.iglesia.repository.IglesiaRepository;

/**
 * Catálogo de iglesias (tenants): dar de alta una iglesia es insertar una fila, sin crear bases ni esquemas. El
 * nombre es único sin distinguir mayúsculas; el índice {@code ux_iglesia_nombre} lo garantiza aunque dos altas
 * coincidan en el tiempo.
 */
@Service
public class IglesiaService {

	private static final Logger LOG = LoggerFactory.getLogger(IglesiaService.class);

	static final String NOMBRE_REPETIDO = "Ya existe una iglesia con ese nombre";

	static final String NO_ENCONTRADA = "Iglesia no encontrada";

	private final IglesiaRepository iglesias;

	private final IglesiaMapper mapper;

	private final UsuarioActual usuarioActual;

	IglesiaService(IglesiaRepository iglesias, IglesiaMapper mapper, UsuarioActual usuarioActual) {
		this.iglesias = iglesias;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<IglesiaResponse> listar() {
		return iglesias.findAllByOrderByNombreAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional(readOnly = true)
	public IglesiaResponse obtener(Long id) {
		return mapper.aRespuesta(buscar(id));
	}

	@Transactional
	public IglesiaResponse crear(GuardarIglesiaRequest solicitud) {
		String nombre = solicitud.nombre().strip();
		if (iglesias.existsByNombreIgnoreCase(nombre)) {
			throw new ConflictoException(NOMBRE_REPETIDO);
		}
		Iglesia iglesia = guardar(Iglesia.nueva(nombre));
		LOG.info("Iglesia creada id={} por usuarioId={}", iglesia.getId(), usuarioActual.id().orElse(null));
		return mapper.aRespuesta(iglesia);
	}

	@Transactional
	public IglesiaResponse renombrar(Long id, GuardarIglesiaRequest solicitud) {
		Iglesia iglesia = buscar(id);
		String nombre = solicitud.nombre().strip();
		if (iglesias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
			throw new ConflictoException(NOMBRE_REPETIDO);
		}
		iglesia.renombrar(nombre);
		guardar(iglesia);
		LOG.info("Iglesia renombrada id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return mapper.aRespuesta(iglesia);
	}

	private Iglesia buscar(Long id) {
		return iglesias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
	}

	/** Escribe ya para que un nombre repetido por un alta simultánea responda 409 y no 500. */
	private Iglesia guardar(Iglesia iglesia) {
		try {
			return iglesias.saveAndFlush(iglesia);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(NOMBRE_REPETIDO);
		}
	}

}
