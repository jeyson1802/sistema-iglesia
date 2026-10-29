package com.iglesia.api.grupos.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.model.DiaSemana;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.repository.GrupoRepository;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

/**
 * Lo que la asistencia necesita de los grupos: cuáles se reúnen un día, cuáles lidera un miembro y con qué nombre se
 * muestran. La asistencia depende de los grupos y no al revés.
 */
@Service
public class AgendaGrupos {

	private final GrupoRepository grupos;

	private final ReferenciasMiembro miembros;

	private final ConfiguracionIglesiaService configuracion;

	AgendaGrupos(GrupoRepository grupos, ReferenciasMiembro miembros, ConfiguracionIglesiaService configuracion) {
		this.grupos = grupos;
		this.miembros = miembros;
		this.configuracion = configuracion;
	}

	/** Grupos activos que se reúnen ese día de la semana. */
	@Transactional(readOnly = true)
	public List<GrupoAgenda> activosDelDia(DiaSemana dia) {
		return agendas(grupos.findByDiaReunionAndActivoTrue(dia));
	}

	@Transactional(readOnly = true)
	public Optional<GrupoAgenda> buscar(Long id) {
		return grupos.findById(id).map(grupo -> agendas(List.of(grupo)).getFirst());
	}

	@Transactional(readOnly = true)
	public List<GrupoAgenda> buscarTodos(Collection<Long> ids) {
		return ids.isEmpty() ? List.of() : agendas(grupos.findAllById(ids));
	}

	/** Ids de los grupos (activos o no) que lidera el miembro: el ámbito de asistencia del líder. */
	@Transactional(readOnly = true)
	public Set<Long> lideradosPor(Long miembroId) {
		return grupos.findByLiderId(miembroId).stream().map(Grupo::getId).collect(Collectors.toSet());
	}

	private List<GrupoAgenda> agendas(List<Grupo> encontrados) {
		String tipo = configuracion.obtener().nombreGrupoSingular();
		Map<Long, ReferenciaResponse> lideres = miembros
			.referencias(encontrados.stream().map(Grupo::getLiderId).toList());
		return encontrados.stream().map(grupo -> {
			ReferenciaResponse lider = lideres.get(grupo.getLiderId());
			String nombre = lider == null ? tipo : tipo + " de " + lider.nombre();
			return new GrupoAgenda(grupo.getId(), grupo.getLiderId(), nombre, grupo.getDiaReunion(),
					grupo.getHoraReunion(), grupo.isActivo());
		}).toList();
	}

}
