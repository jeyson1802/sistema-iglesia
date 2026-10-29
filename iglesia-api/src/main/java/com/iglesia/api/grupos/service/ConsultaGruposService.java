package com.iglesia.api.grupos.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.mapper.GrupoMapper;
import com.iglesia.api.grupos.repository.GrupoRepository;
import com.iglesia.api.miembros.service.ConsultaGrupos;

/** Lo que el registro de miembros necesita de los grupos. Solo usa el repositorio, para no crear ciclos de beans. */
@Service
class ConsultaGruposService implements ConsultaGrupos {

	static final String CAMPO_GRUPO = "grupoId";

	static final String INEXISTENTE = "El grupo no existe";

	static final String INACTIVO = "El grupo está desactivado y no se puede asignar";

	private final GrupoRepository grupos;

	private final GrupoMapper mapper;

	ConsultaGruposService(GrupoRepository grupos, GrupoMapper mapper) {
		this.grupos = grupos;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public void validarAsignable(Long nuevoId, Long actualId) {
		if (nuevoId == null || Objects.equals(nuevoId, actualId)) {
			return;
		}
		Grupo grupo = grupos.findById(nuevoId).orElseThrow(() -> new ConflictoException(INEXISTENTE, CAMPO_GRUPO));
		if (!grupo.isActivo()) {
			throw new ConflictoException(INACTIVO, CAMPO_GRUPO);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, ReferenciaResponse> referencias(Collection<Long> ids) {
		var validos = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (validos.isEmpty()) {
			return Collections.emptyMap();
		}
		return grupos.findAllById(validos)
			.stream()
			.map(mapper::aReferencia)
			.collect(Collectors.toMap(ReferenciaResponse::id, Function.identity()));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean lideraAlgunGrupoActivo(Long miembroId) {
		return grupos.existsByLiderIdAndActivoTrue(miembroId);
	}

}
