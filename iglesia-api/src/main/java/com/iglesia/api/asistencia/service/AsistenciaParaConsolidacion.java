package com.iglesia.api.asistencia.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.ReunionAsistidaResponse;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.FechaAsistida;
import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.asistencia.repository.AsistenciaRepository;
import com.iglesia.api.asistencia.repository.ReunionRepository;

/**
 * Lo que la consolidación necesita de la asistencia: el resumen de una persona, quiénes tienen una alerta (para armar
 * un lote) y a qué reuniones asistieron (para medir si volvieron después de un contacto). No aplica el ámbito de
 * asistencia: la consolidación tiene sus propios permisos y decide antes quién puede ver a quién.
 */
@Service
public class AsistenciaParaConsolidacion {

	/** Tope de reuniones de la línea de tiempo de un caso. */
	static final int ASISTENCIAS_MAX = 100;

	private final SeguimientoAsistenciaService seguimiento;

	private final AsistenciaRepository asistencias;

	private final ReunionRepository reuniones;

	private final AsistenciaMapper mapper;

	AsistenciaParaConsolidacion(SeguimientoAsistenciaService seguimiento, AsistenciaRepository asistencias,
			ReunionRepository reuniones, AsistenciaMapper mapper) {
		this.seguimiento = seguimiento;
		this.asistencias = asistencias;
		this.reuniones = reuniones;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public AsistenciaMiembroResponse resumen(Long miembroId) {
		return seguimiento.resumenDe(miembroId);
	}

	@Transactional(readOnly = true)
	public Set<Long> conAlerta(TipoAlerta alerta) {
		return seguimiento.idsConAlerta(alerta);
	}

	/** Fechas en que asistió cada miembro desde {@code desde}; quien no asistió no aparece. */
	@Transactional(readOnly = true)
	public Map<Long, List<LocalDate>> fechasDesde(Collection<Long> miembroIds, LocalDate desde) {
		if (miembroIds.isEmpty()) {
			return Map.of();
		}
		return asistencias.fechasAsistidas(miembroIds, desde)
			.stream()
			.collect(Collectors.groupingBy(FechaAsistida::miembroId,
					Collectors.mapping(FechaAsistida::fecha, Collectors.toList())));
	}

	/** Reuniones a las que asistió entre dos fechas, de la más reciente a la más antigua. */
	@Transactional(readOnly = true)
	public List<ReunionAsistidaResponse> reunionesEntre(Long miembroId, LocalDate desde, LocalDate hasta) {
		return reuniones.asistidasEntre(miembroId, desde, hasta, PageRequest.of(0, ASISTENCIAS_MAX))
			.stream()
			.map(mapper::aAsistida)
			.toList();
	}

}
