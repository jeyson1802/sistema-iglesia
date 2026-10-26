package com.iglesia.seguridad.usuario.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.entity.Usuario;

@Component
public class UsuarioMapper {

	public UsuarioResponse aRespuesta(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getIglesiaId(), usuario.getMiembroId(),
				usuario.isActivo(), usuario.getRolesBase(), usuario.getCreadoEn());
	}

}
