package com.iglesia.api.grupos.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Component
public class GrupoMapper {

	public GrupoResponse aRespuesta(Grupo grupo, ReferenciaResponse lider, ReferenciaResponse anfitrion,
			UbicacionResponse ubicacion, long cantidadMiembros) {
		return new GrupoResponse(grupo.getId(), grupo.getNombre(), lider, anfitrion, grupo.getDiaReunion(),
				grupo.getHoraReunion(), grupo.getDireccion(), grupo.getDistritoCodigo(), ubicacion,
				grupo.getDescripcion(), grupo.isActivo(), cantidadMiembros);
	}

	public ReferenciaResponse aReferencia(Grupo grupo) {
		return new ReferenciaResponse(grupo.getId(), grupo.getNombre(), grupo.isActivo());
	}

}
