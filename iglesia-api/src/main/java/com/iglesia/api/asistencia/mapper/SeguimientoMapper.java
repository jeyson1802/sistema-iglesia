package com.iglesia.api.asistencia.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.iglesia.api.asistencia.dto.OcasionResponse;
import com.iglesia.api.asistencia.dto.PersonaSeguimientoResponse;
import com.iglesia.api.asistencia.model.Asistidas;
import com.iglesia.api.asistencia.model.EstadoOcasion;
import com.iglesia.api.asistencia.model.IndicadoresMiembro;
import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.repository.SeguimientoRepository.FilaIndicadores;
import com.iglesia.api.asistencia.repository.SeguimientoRepository.FilaOcasion;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.miembros.model.MiembroAsistente;

/** Conversión de las filas del seguimiento al modelo de dominio y a los DTOs. */
@Component
public class SeguimientoMapper {

	public IndicadoresMiembro aIndicadores(FilaIndicadores fila) {
		return new IndicadoresMiembro(fila.getMiembroId(), fila.getGrupoId(), fila.getLiderId(), fila.getInicio(),
				fila.getNuevo(), new Asistidas(fila.getEsperadas4(), fila.getAsistidas4()),
				new Asistidas(fila.getEsperadas8(), fila.getAsistidas8()),
				new Asistidas(fila.getEsperadas5a12(), fila.getAsistidas5a12()),
				new Asistidas(fila.getEsperadas9a16(), fila.getAsistidas9a16()), fila.getFaltasSeguidas(),
				fila.getUltimaAsistencia(), fila.getDiasAsistidos());
	}

	public OcasionResponse aOcasion(FilaOcasion fila) {
		return new OcasionResponse(fila.getFecha(), fila.getHora(), TipoReunion.valueOf(fila.getTipo()),
				fila.getDescripcion(), fila.getAsistioA(), EstadoOcasion.valueOf(fila.getEstado()));
	}

	/**
	 * @param miembros nombre y nivel de cada miembro y de su líder, por id
	 * @param grupos grupos por id
	 */
	public PersonaSeguimientoResponse aPersona(IndicadoresMiembro indicadores, List<TipoAlerta> alertas,
			Map<Long, MiembroAsistente> miembros, Map<Long, GrupoAgenda> grupos) {
		MiembroAsistente miembro = miembros.get(indicadores.miembroId());
		MiembroAsistente lider = buscar(miembros, indicadores.liderId());
		GrupoAgenda grupo = buscar(grupos, indicadores.grupoId());
		LocalDate ultima = indicadores.ultimaAsistencia();
		return new PersonaSeguimientoResponse(indicadores.miembroId(),
				miembro == null ? null : miembro.nombreCompleto(), miembro == null ? null : miembro.nivel(),
				indicadores.grupoId(), grupo == null ? null : grupo.nombre(),
				lider == null ? null : lider.nombreCompleto(), indicadores.ultimas8().porcentaje(),
				indicadores.semanas9a16().porcentaje(), indicadores.faltasSeguidas(), ultima, alertas);
	}

	/** Algunos mapas no admiten buscar {@code null} (sin líder o sin grupo). */
	private static <T> T buscar(Map<Long, T> porId, Long id) {
		return id == null ? null : porId.get(id);
	}

}
