package com.iglesia.seguridad.usuario.service;

import java.util.Objects;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

/**
 * Sobre quién se actúa: el administrador del sistema en la vista global, sobre todos; el resto (incluido el
 * administrador del sistema dentro de una iglesia), solo sobre usuarios de la iglesia en la que opera y sin otorgar
 * el rol base de administrador del sistema. Qué acción se puede hacer no
 * lo decide esta política sino el permiso exigido en cada endpoint.
 */
@Component
class PoliticaAdministracionUsuarios {

	boolean puedeAdministrar(UsuarioAutenticado actor, Long iglesiaId, Set<RolBase> rolesBase) {
		if (actor.esGlobal()) {
			return true;
		}
		return iglesiaId != null && Objects.equals(actor.iglesiaId(), iglesiaId)
				&& !rolesBase.contains(RolBase.ADMIN_SISTEMA);
	}

	boolean puedeVer(UsuarioAutenticado actor, Usuario usuario) {
		return puedeAdministrar(actor, usuario.getIglesiaId(), usuario.getRolesBase());
	}

	void verificarPuedeAdministrar(UsuarioAutenticado actor, Long iglesiaId, Set<RolBase> rolesBase) {
		if (!puedeAdministrar(actor, iglesiaId, rolesBase)) {
			throw new AccessDeniedException("Sin permisos para administrar el usuario");
		}
	}

	/**
	 * Iglesia por la que se filtra un listado. Quien opera en una iglesia siempre ve esa, sin importar el
	 * filtro enviado; el administrador del sistema en la vista global usa el filtro (nulo = todas).
	 */
	Long iglesiaParaListado(UsuarioAutenticado actor, Long iglesiaSolicitada) {
		if (actor.esGlobal()) {
			return iglesiaSolicitada;
		}
		if (actor.iglesiaId() != null) {
			return actor.iglesiaId();
		}
		throw new AccessDeniedException("Sin permisos para listar usuarios");
	}

}
