package com.iglesia.api.iglesia.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;

@Component
public class IglesiaMapper {

	public IglesiaResponse aRespuesta(Iglesia iglesia) {
		return new IglesiaResponse(iglesia.getId(), iglesia.getNombre(), iglesia.getCreadaEn());
	}

}
