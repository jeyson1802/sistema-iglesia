package com.iglesia.api.aportes.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.aportes.dto.CategoriaAporteResponse;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Component
public class CategoriaAporteMapper {

	public CategoriaAporteResponse aRespuesta(CategoriaAporte categoria) {
		return new CategoriaAporteResponse(categoria.getId(), categoria.getNombre(), categoria.isExigeAportante(),
				categoria.getOrden(), categoria.isActivo());
	}

	public ReferenciaResponse aReferencia(CategoriaAporte categoria) {
		return new ReferenciaResponse(categoria.getId(), categoria.getNombre(), categoria.isActivo());
	}

}
