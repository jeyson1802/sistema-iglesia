package com.iglesia.api.asistencia.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.asistencia.dto.AsistenteResponse;
import com.iglesia.api.asistencia.dto.CultoProgramadoResponse;
import com.iglesia.api.asistencia.dto.MarcaResponse;
import com.iglesia.api.asistencia.dto.MiembroReunionResponse;
import com.iglesia.api.asistencia.dto.ReunionAsistidaResponse;
import com.iglesia.api.asistencia.dto.ReunionHoyResponse;
import com.iglesia.api.asistencia.dto.ReunionResponse;
import com.iglesia.api.asistencia.entity.Asistencia;
import com.iglesia.api.asistencia.entity.CultoProgramado;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.miembros.model.MiembroAsistente;

@Component
public class AsistenciaMapper {

	public CultoProgramadoResponse aRespuesta(CultoProgramado culto) {
		return new CultoProgramadoResponse(culto.getId(), culto.getNombre(), culto.getDiaSemana(), culto.getHora(),
				culto.isActivo());
	}

	public ReunionResponse aRespuesta(Reunion reunion, long totalAsistentes) {
		return new ReunionResponse(reunion.getId(), reunion.getTipo(), reunion.getNombre(), reunion.getFecha(),
				reunion.getHora(), reunion.getCultoProgramadoId(), reunion.getGrupoId(), totalAsistentes);
	}

	public ReunionHoyResponse aHoy(Reunion reunion, long totalAsistentes) {
		return new ReunionHoyResponse(reunion.getId(), reunion.getTipo(), reunion.getNombre(), reunion.getHora(),
				reunion.getCultoProgramadoId(), reunion.getGrupoId(), totalAsistentes);
	}

	public ReunionHoyResponse aHoyPorAbrir(CultoProgramado culto) {
		return new ReunionHoyResponse(null, TipoReunion.CULTO, culto.getNombre(), culto.getHora(), culto.getId(), null,
				0);
	}

	public ReunionHoyResponse aHoyPorAbrir(GrupoAgenda grupo) {
		return new ReunionHoyResponse(null, TipoReunion.GRUPO, grupo.nombre(), grupo.horaReunion(), null, grupo.id(),
				0);
	}

	public ReunionAsistidaResponse aAsistida(Reunion reunion) {
		return new ReunionAsistidaResponse(reunion.getId(), reunion.getTipo(), reunion.getNombre(), reunion.getFecha());
	}

	public MarcaResponse aMarca(MiembroAsistente miembro, Asistencia asistencia, boolean yaRegistrado, long total) {
		return new MarcaResponse(miembro.id(), miembro.nombreCompleto(), miembro.nivel(), miembro.grupoId(),
				yaRegistrado, asistencia.getRegistradaEn(), total);
	}

	/** Si el miembro ya no existe (no debería, la FK lo impide) se muestra sin nombre en lugar de fallar. */
	public AsistenteResponse aAsistente(Asistencia asistencia, MiembroAsistente miembro) {
		return new AsistenteResponse(asistencia.getMiembroId(), miembro == null ? null : miembro.nombreCompleto(),
				miembro == null ? null : miembro.nivel(), miembro == null ? null : miembro.grupoId(),
				asistencia.getMedio(), asistencia.getRegistradaEn());
	}

	public MiembroReunionResponse aMiembroReunion(MiembroAsistente miembro, boolean integrante, boolean presente) {
		return new MiembroReunionResponse(miembro.id(), miembro.nombreCompleto(), miembro.nivel(), miembro.grupoId(),
				integrante, presente);
	}

}
