package com.iglesia.seguridad.usuario;

import org.springframework.stereotype.Component;

import com.iglesia.seguridad.usuario.dto.UsuarioResponse;

@Component
class UsuarioMapper {

	UsuarioResponse aRespuesta(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getIglesiaId(), usuario.getMiembroId(),
				usuario.isActivo(), usuario.getRoles(), usuario.getCreadoEn());
	}

}
