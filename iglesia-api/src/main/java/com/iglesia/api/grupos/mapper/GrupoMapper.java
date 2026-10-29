package com.iglesia.api.grupos.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.dto.CandidatoGrupoResponse;
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.dto.IntegranteGrupoResponse;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Component
public class GrupoMapper {

	public GrupoResponse aRespuesta(Grupo grupo, ReferenciaResponse lider, ReferenciaResponse anfitrion,
			UbicacionResponse ubicacion, long cantidadMiembros) {
		return new GrupoResponse(grupo.getId(), lider, anfitrion, grupo.getDiaReunion(),
				grupo.getHoraReunion(), grupo.getDireccion(), grupo.getDistritoCodigo(), ubicacion,
				grupo.getDescripcion(), grupo.isActivo(), cantidadMiembros);
	}

	/** Referencia al grupo cuyo nombre es el de su líder: el frontend la muestra como «<tipo> de <líder>». */
	public ReferenciaResponse aReferencia(Grupo grupo, ReferenciaResponse lider) {
		return new ReferenciaResponse(grupo.getId(), lider == null ? null : lider.nombre(), grupo.isActivo());
	}

	public IntegranteGrupoResponse aIntegrante(IntegranteGrupo integrante) {
		return new IntegranteGrupoResponse(integrante.id(), integrante.nombreCompleto(), integrante.nivel());
	}

	public CandidatoGrupoResponse aCandidato(IntegranteGrupo integrante, ReferenciaResponse grupo) {
		return new CandidatoGrupoResponse(integrante.id(), integrante.nombreCompleto(), integrante.nivel(), grupo);
	}

}
