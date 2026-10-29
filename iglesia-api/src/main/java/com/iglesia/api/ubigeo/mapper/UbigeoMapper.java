package com.iglesia.api.ubigeo.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.dto.UbigeoResponse;
import com.iglesia.api.ubigeo.entity.Departamento;
import com.iglesia.api.ubigeo.entity.Distrito;
import com.iglesia.api.ubigeo.entity.Provincia;

@Component
public class UbigeoMapper {

	public UbigeoResponse aRespuesta(Departamento departamento) {
		return new UbigeoResponse(departamento.getCodigo(), departamento.getNombre());
	}

	public UbigeoResponse aRespuesta(Provincia provincia) {
		return new UbigeoResponse(provincia.getCodigo(), provincia.getNombre());
	}

	public UbigeoResponse aRespuesta(Distrito distrito) {
		return new UbigeoResponse(distrito.getCodigo(), distrito.getNombre());
	}

	public UbicacionResponse aUbicacion(Distrito distrito) {
		Provincia provincia = distrito.getProvincia();
		return new UbicacionResponse(aRespuesta(provincia.getDepartamento()), aRespuesta(provincia),
				aRespuesta(distrito));
	}

}
