package com.iglesia.api.asistencia.mapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.iglesia.api.asistencia.dto.DetalleReunionGrupoResponse;
import com.iglesia.api.asistencia.dto.PersonaReunionGrupoResponse;
import com.iglesia.api.asistencia.dto.ReunionGrupoResponse;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.model.Asistidas;
import com.iglesia.api.asistencia.model.EstadoEnReunion;
import com.iglesia.api.asistencia.repository.AsistenciaGrupoRepository.FilaPersona;
import com.iglesia.api.asistencia.repository.AsistenciaGrupoRepository.FilaReunion;

/** Conversión de las filas de la asistencia de un grupo a los DTOs. */
@Component
public class AsistenciaGrupoMapper {

	public ReunionGrupoResponse aReunion(FilaReunion fila) {
		long esperados = fila.getAsistieron() + fila.getFaltaron();
		return new ReunionGrupoResponse(fila.getReunionId(), fila.getFecha(), fila.getHora(), esperados,
				fila.getAsistieron(), fila.getFaltaron(), fila.getVisitantes(),
				new Asistidas(esperados, fila.getAsistieron()).porcentaje());
	}

	/** Reparte a las personas según cómo quedaron, sin cambiar su orden (por nombre). */
	public DetalleReunionGrupoResponse aDetalle(Reunion reunion, List<FilaPersona> filas) {
		Map<EstadoEnReunion, List<PersonaReunionGrupoResponse>> porEstado = filas.stream()
			.collect(Collectors.groupingBy(fila -> EstadoEnReunion.valueOf(fila.getEstado()),
					() -> new EnumMap<>(EstadoEnReunion.class),
					Collectors.mapping(this::aPersona, Collectors.toList())));
		return new DetalleReunionGrupoResponse(reunion.getId(), reunion.getFecha(), reunion.getHora(),
				porEstado.getOrDefault(EstadoEnReunion.ASISTIO, List.of()),
				porEstado.getOrDefault(EstadoEnReunion.FALTO, List.of()),
				porEstado.getOrDefault(EstadoEnReunion.VISITANTE, List.of()));
	}

	private PersonaReunionGrupoResponse aPersona(FilaPersona fila) {
		return new PersonaReunionGrupoResponse(fila.getMiembroId(), fila.getNombreCompleto(), fila.getNivel());
	}

}
