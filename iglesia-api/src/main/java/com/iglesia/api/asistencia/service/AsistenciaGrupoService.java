package com.iglesia.api.asistencia.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.DetalleReunionGrupoResponse;
import com.iglesia.api.asistencia.dto.ReunionGrupoResponse;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.mapper.AsistenciaGrupoMapper;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.repository.AsistenciaGrupoRepository;
import com.iglesia.api.asistencia.repository.ReunionRepository;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.grupos.service.AgendaGrupos;

/**
 * Asistencia de un grupo reunión por reunión: el resumen de cada una y quiénes asistieron, faltaron o vinieron de
 * visita. Con {@code asistencia.ver}, cualquier grupo; con {@code asistencia.grupo}, solo los que lidera.
 */
@Service
public class AsistenciaGrupoService {

	private final AsistenciaGrupoRepository consultas;

	private final ReunionRepository reuniones;

	private final AgendaGrupos grupos;

	private final AmbitoAsistencia ambito;

	private final AsistenciaGrupoMapper mapper;

	private final IglesiaActual iglesiaActual;

	AsistenciaGrupoService(AsistenciaGrupoRepository consultas, ReunionRepository reuniones, AgendaGrupos grupos,
			AmbitoAsistencia ambito, AsistenciaGrupoMapper mapper, IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.reuniones = reuniones;
		this.grupos = grupos;
		this.ambito = ambito;
		this.mapper = mapper;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public Page<ReunionGrupoResponse> reuniones(Long grupoId, Pageable paginacion) {
		verificarGrupoVisible(grupoId);
		return consultas.reuniones(iglesiaActual.requerida(), grupoId, paginacion).map(mapper::aReunion);
	}

	@Transactional(readOnly = true)
	public DetalleReunionGrupoResponse detalle(Long grupoId, Long reunionId) {
		verificarGrupoVisible(grupoId);
		Reunion reunion = reuniones.findById(reunionId)
			.filter(encontrada -> encontrada.getTipo() == TipoReunion.GRUPO && grupoId.equals(encontrada.getGrupoId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(ReunionService.NO_ENCONTRADA));
		return mapper.aDetalle(reunion,
				consultas.personas(iglesiaActual.requerida(), grupoId, reunionId, reunion.getFecha()));
	}

	private void verificarGrupoVisible(Long grupoId) {
		grupos.buscar(grupoId)
			.filter(grupo -> ambito.paraConsultar().incluyeGrupo(grupo.id()))
			.orElseThrow(() -> new RecursoNoEncontradoException(SeguimientoAsistenciaService.GRUPO_NO_ENCONTRADO));
	}

}
