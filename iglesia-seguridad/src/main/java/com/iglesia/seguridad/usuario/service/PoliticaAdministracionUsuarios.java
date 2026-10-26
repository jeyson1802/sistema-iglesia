package com.iglesia.seguridad.usuario;

import java.util.Objects;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.common.seguridad.UsuarioAutenticado;

/**
 * Quién puede administrar a quién: el administrador del sistema, a todos; el administrador de
 * iglesia, solo a usuarios de su propia iglesia y sin otorgar el rol de administrador del sistema.
 */
@Component
class PoliticaAdministracionUsuarios {

	boolean puedeAdministrar(UsuarioAutenticado actor, Long iglesiaId, Set<Rol> roles) {
		if (actor.tieneRol(Rol.ADMIN_SISTEMA)) {
			return true;
		}
		return actor.tieneRol(Rol.ADMIN_IGLESIA) && iglesiaId != null
				&& Objects.equals(actor.iglesiaId(), iglesiaId) && !roles.contains(Rol.ADMIN_SISTEMA);
	}

	boolean puedeVer(UsuarioAutenticado actor, Usuario usuario) {
		return puedeAdministrar(actor, usuario.getIglesiaId(), usuario.getRoles());
	}

	void verificarPuedeAdministrar(UsuarioAutenticado actor, Long iglesiaId, Set<Rol> roles) {
		if (!puedeAdministrar(actor, iglesiaId, roles)) {
			throw new AccessDeniedException("Sin permisos para administrar el usuario");
		}
	}

	/**
	 * Iglesia por la que se filtra un listado. El administrador de iglesia siempre ve la suya,
	 * sin importar el filtro enviado; el del sistema usa el filtro (nulo = todas).
	 */
	Long iglesiaParaListado(UsuarioAutenticado actor, Long iglesiaSolicitada) {
		if (actor.tieneRol(Rol.ADMIN_SISTEMA)) {
			return iglesiaSolicitada;
		}
		if (actor.tieneRol(Rol.ADMIN_IGLESIA) && actor.iglesiaId() != null) {
			return actor.iglesiaId();
		}
		throw new AccessDeniedException("Sin permisos para listar usuarios");
	}

}
