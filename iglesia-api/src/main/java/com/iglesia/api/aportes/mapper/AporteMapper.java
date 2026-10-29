package com.iglesia.api.aportes.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.aportes.dto.AporteResponse;
import com.iglesia.api.aportes.entity.Aporte;
import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Component
public class AporteMapper {

	private final CategoriaAporteMapper categorias;

	public AporteMapper(CategoriaAporteMapper categorias) {
		this.categorias = categorias;
	}

	/** El aportante llega resuelto (por lotes) desde el contexto de miembros; {@code null} si es anónimo. */
	public AporteResponse aRespuesta(Aporte aporte, ReferenciaResponse miembro) {
		return new AporteResponse(aporte.getId(), aporte.getFecha(), miembro,
				categorias.aReferencia(aporte.getCategoria()), aporte.getMonto(), aporte.getMedio(),
				aporte.getNumeroOperacion(), aporte.getPeticion(), aporte.getObservacion(), aporte.isAnulado(),
				aporte.getMotivoAnulacion(), aporte.getRegistradoEn(), aporte.getModificadoEn());
	}

}
