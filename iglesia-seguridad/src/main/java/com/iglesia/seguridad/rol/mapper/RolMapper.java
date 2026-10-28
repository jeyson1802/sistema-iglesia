package com.iglesia.seguridad.rol.mapper;

import org.springframework.stereotype.Component;

import com.iglesia.seguridad.rol.dto.PermisoResponse;
import com.iglesia.seguridad.rol.dto.RolResponse;
import com.iglesia.seguridad.rol.entity.Permiso;
import com.iglesia.seguridad.rol.entity.Rol;

@Component
public class RolMapper {

	public RolResponse aRespuesta(Rol rol, long cantidadUsuarios) {
		return new RolResponse(rol.getId(), rol.getNombre(), rol.rolBaseEfectivo(), rol.esBase(), rol.isActivo(),
				rol.rolBaseEfectivo().requiereMiembro(), rol.getPermisos().stream().map(Permiso::getCodigo).sorted().toList(), cantidadUsuarios);
	}

	public PermisoResponse aRespuesta(Permiso permiso) {
		return new PermisoResponse(permiso.getCodigo(), permiso.getModulo(), permiso.getDescripcion());
	}

}
