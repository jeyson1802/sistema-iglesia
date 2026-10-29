package com.iglesia.api.niveles.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.niveles.dto.NivelResponse;
import com.iglesia.api.niveles.entity.Nivel;

@Component
public class NivelMapper {

	public NivelResponse aRespuesta(Nivel nivel) {
		return new NivelResponse(nivel.getId(), nivel.getNombre(), nivel.getOrden(), nivel.isActivo());
	}

	public ReferenciaResponse aReferencia(Nivel nivel) {
		return new ReferenciaResponse(nivel.getId(), nivel.getNombre(), nivel.isActivo());
	}

}
