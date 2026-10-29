package com.iglesia.api.iglesia.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.model.Apariencia;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Component
public class IglesiaMapper {

	/** La ubicación la resuelve el servicio (una consulta por página, no una por iglesia). */
	public IglesiaResponse aRespuesta(Iglesia iglesia, UbicacionResponse ubicacion) {
		return new IglesiaResponse(iglesia.getId(), iglesia.getNombreComercial(), iglesia.getDenominacion(),
				iglesia.getDireccion(), iglesia.getDistritoCodigo(), ubicacion, iglesia.getTelefono(),
				iglesia.getCorreo(), iglesia.getSitioWeb(), iglesia.isActiva(), iglesia.tieneLogo(),
				iglesia.getLogoActualizadoEn(), aRespuesta(iglesia.getApariencia()), iglesia.getCreadaEn());
	}

	public AparienciaResponse aRespuesta(Apariencia apariencia) {
		return new AparienciaResponse(apariencia.preset(), apariencia.colorPrimario(), apariencia.colorSuperficie(),
				apariencia.tipografia());
	}

}
