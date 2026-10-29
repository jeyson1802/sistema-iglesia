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
import com.iglesia.api.miembros.service.ReferenciasMiembro;

/**
 * Lo que el registro de miembros necesita de los grupos. Usa el repositorio y {@link ReferenciasMiembro} (que no
 * depende de los grupos), no {@code MiembroService}, para no crear ciclos de beans.
 */
@Service
class ConsultaGruposService implements ConsultaGrupos {

	static final String CAMPO_GRUPO = "grupoId";

	static final String INEXISTENTE = "El grupo no existe";

	static final String INACTIVO = "El grupo está desactivado y no se puede asignar";

	private final GrupoRepository grupos;

	private final GrupoMapper mapper;

	private final ReferenciasMiembro miembros;

	ConsultaGruposService(GrupoRepository grupos, GrupoMapper mapper, ReferenciasMiembro miembros) {
		this.grupos = grupos;
		this.mapper = mapper;
		this.miembros = miembros;
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
		var encontrados = grupos.findAllById(validos);
		Map<Long, ReferenciaResponse> lideres = miembros
			.referencias(encontrados.stream().map(Grupo::getLiderId).toList());
		return encontrados.stream()
			.map(grupo -> mapper.aReferencia(grupo, lideres.get(grupo.getLiderId())))
			.collect(Collectors.toMap(ReferenciaResponse::id, Function.identity()));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean lideraAlgunGrupoActivo(Long miembroId) {
		return grupos.existsByLiderIdAndActivoTrue(miembroId);
	}

}
