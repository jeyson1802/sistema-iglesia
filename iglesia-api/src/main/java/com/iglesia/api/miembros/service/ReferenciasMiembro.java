package com.iglesia.api.miembros.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.mapper.MiembroMapper;
import com.iglesia.api.miembros.model.ConteoPorGrupo;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.miembros.repository.EspecificacionesMiembro;
import com.iglesia.api.miembros.repository.MiembroRepository;

/**
 * Consultas sobre miembros que usan otros contextos (grupos) y el propio registro: validar a quién se puede
 * referenciar y obtener nombres y conteos por lotes, sin exponer la entidad fuera del servicio.
 */
@Service
public class ReferenciasMiembro {

	static final String INEXISTENTE = "El miembro indicado no existe";

	static final String INACTIVO = "El miembro indicado está desactivado y no se puede asignar";

	static final String NO_ENCONTRADO = "Miembro no encontrado";

	static final String NO_ESTA_EN_EL_GRUPO = "El miembro no pertenece a ese grupo";

	static final String INACTIVO_EN_GRUPO = "El miembro está desactivado y no se puede agregar a un grupo";

	private static final Sort ORDEN_POR_NOMBRE = Sort.by("apellidos", "nombres", "id");

	private final MiembroRepository miembros;

	private final MiembroMapper mapper;

	ReferenciasMiembro(MiembroRepository miembros, MiembroMapper mapper) {
		this.miembros = miembros;
		this.mapper = mapper;
	}

	/**
	 * Miembro que se referencia desde {@code campo}. Si no cambia respecto del actual se acepta aunque esté inactivo
	 * (las referencias ya asignadas se conservan); si cambia, debe existir en la iglesia y estar activo (409).
	 */
	@Transactional(readOnly = true)
	public Miembro asignable(Long nuevoId, Miembro actual, String campo) {
		if (nuevoId == null) {
			return null;
		}
		if (actual != null && Objects.equals(actual.getId(), nuevoId)) {
			return actual;
		}
		Miembro miembro = miembros.findById(nuevoId).orElseThrow(() -> new ConflictoException(INEXISTENTE, campo));
		if (!miembro.isActivo()) {
			throw new ConflictoException(INACTIVO, campo);
		}
		return miembro;
	}

	/** Variante para quien guarda la referencia por id (p. ej. el líder y el anfitrión de un grupo). */
	@Transactional(readOnly = true)
	public void validarAsignable(Long nuevoId, Long actualId, String campo) {
		if (nuevoId != null && !Objects.equals(nuevoId, actualId)) {
			asignable(nuevoId, null, campo);
		}
	}

	@Transactional(readOnly = true)
	public Map<Long, ReferenciaResponse> referencias(Collection<Long> ids) {
		var validos = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (validos.isEmpty()) {
			return Collections.emptyMap();
		}
		return miembros.findAllById(validos)
			.stream()
			.map(mapper::aReferencia)
			.collect(Collectors.toMap(ReferenciaResponse::id, Function.identity()));
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> activosPorGrupo(Collection<Long> grupoIds) {
		if (grupoIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return miembros.contarActivosPorGrupo(grupoIds)
			.stream()
			.collect(Collectors.toMap(ConteoPorGrupo::grupoId, ConteoPorGrupo::cantidad));
	}

	@Transactional(readOnly = true)
	public boolean hayActivosEnGrupo(Long grupoId) {
		return miembros.existsByGrupoIdAndActivoTrue(grupoId);
	}

	/** Integrantes activos del grupo, ordenados por apellidos. */
	@Transactional(readOnly = true)
	public List<IntegranteGrupo> integrantesDe(Long grupoId) {
		return miembros.findByGrupoIdAndActivoTrueOrderByApellidosAscNombresAscIdAsc(grupoId)
			.stream()
			.map(mapper::aIntegrante)
			.toList();
	}

	/**
	 * Hasta {@value LimitesMiembro#OPCIONES_MAX} miembros activos que no están en el grupo y cuyo nombre contiene el
	 * texto, con el grupo en el que están ahora (o ninguno).
	 */
	@Transactional(readOnly = true)
	public List<IntegranteGrupo> candidatosPara(Long grupoId, String q) {
		Specification<Miembro> criterio = Specification.allOf(EspecificacionesMiembro.nombreContiene(q),
				EspecificacionesMiembro.conEstado(true), EspecificacionesMiembro.fueraDelGrupo(grupoId));
		return miembros.findAll(criterio, PageRequest.of(0, LimitesMiembro.OPCIONES_MAX, ORDEN_POR_NOMBRE))
			.map(mapper::aIntegrante)
			.getContent();
	}

	/**
	 * Pone al miembro en el grupo: lo agrega o lo mueve desde otro. Un miembro inexistente responde 404 y uno
	 * desactivado, 409. Devuelve el integrante con el grupo que tenía antes, para registrar el movimiento.
	 */
	@Transactional
	public IntegranteGrupo ponerEnGrupo(Long miembroId, Long grupoId) {
		Miembro miembro = buscar(miembroId);
		if (!miembro.isActivo()) {
			throw new ConflictoException(INACTIVO_EN_GRUPO);
		}
		IntegranteGrupo antes = mapper.aIntegrante(miembro);
		miembro.cambiarGrupo(grupoId);
		return antes;
	}

	/** Saca al miembro del grupo; si no existe o no está en ese grupo responde 404. */
	@Transactional
	public void quitarDeGrupo(Long miembroId, Long grupoId) {
		Miembro miembro = buscar(miembroId);
		if (!grupoId.equals(miembro.getGrupoId())) {
			throw new RecursoNoEncontradoException(NO_ESTA_EN_EL_GRUPO);
		}
		miembro.cambiarGrupo(null);
	}

	private Miembro buscar(Long id) {
		return miembros.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

}
