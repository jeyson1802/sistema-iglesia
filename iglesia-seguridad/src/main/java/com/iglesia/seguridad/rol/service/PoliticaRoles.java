package com.iglesia.seguridad.rol.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.Permiso;
import com.iglesia.seguridad.rol.entity.Rol;

/**
 * Reglas de los roles personalizados que no se pueden expresar con restricciones de la base: de qué rol
 * base pueden heredar, qué permisos se les pueden dar, quién los ve y cuándo se pueden desactivar. Qué
 * acción se puede hacer lo decide el permiso de cada endpoint.
 */
@Component
class PoliticaRoles {

	static final String ROL_NO_ENCONTRADO = "Rol no encontrado";

	/** Los roles base son de todos; los personalizados solo los ve su iglesia (el resto, como inexistentes). */
	void verificarVisible(UsuarioAutenticado actor, Rol rol) {
		if (!rol.esBase() && !Objects.equals(rol.getIglesiaId(), actor.iglesiaId())) {
			throw new RecursoNoEncontradoException(ROL_NO_ENCONTRADO);
		}
	}

	/** Los roles personalizados se crean en la iglesia de quien actúa; sin iglesia no se pueden crear. */
	Long iglesiaParaCrear(UsuarioAutenticado actor) {
		if (actor.iglesiaId() == null) {
			throw new AccessDeniedException("Sin iglesia para crear roles personalizados");
		}
		return actor.iglesiaId();
	}

	void verificarHeredable(Rol rolBase) {
		if (!rolBase.esBase() || !rolBase.rolBaseEfectivo().heredable()) {
			throw new ReglaNegocioException(
					"El rol base debe ser Administrador de iglesia, Líder o Miembro, no otro rol personalizado");
		}
	}

	/**
	 * Un rol personalizado puede tener cualquier permiso de iglesia, sea cual sea su rol base (p. ej. «Tesorería» con
	 * todo lo de aportes): el rol base solo decide su página de inicio y su ámbito. Evita la escalada de privilegios:
	 * los permisos asignables son los de los roles base heredables, así que nunca los del sistema (catálogo de
	 * iglesias, estadísticas entre iglesias), que solo tiene {@code ADMIN_SISTEMA}.
	 */
	void verificarPermisosAsignables(Set<String> permisos, Collection<Rol> rolesBaseHeredables) {
		Set<String> asignables = new TreeSet<>();
		rolesBaseHeredables.stream()
			.flatMap(rol -> rol.getPermisos().stream())
			.map(Permiso::getCodigo)
			.forEach(asignables::add);
		Set<String> sobrantes = new TreeSet<>(permisos);
		sobrantes.removeAll(asignables);
		if (!sobrantes.isEmpty()) {
			throw new ReglaNegocioException(
					"Estos permisos no existen o no se pueden dar a un rol de iglesia: " + sobrantes);
		}
	}

	void verificarEditable(Rol rol) {
		if (rol.esBase()) {
			throw new AccessDeniedException("Los roles base no se pueden modificar");
		}
	}

	/** Ningún usuario puede quedar sin roles activos: iniciaría sesión sin ningún perfil. */
	void verificarDesactivable(boolean dejariaUsuariosSinRolesActivos) {
		if (dejariaUsuariosSinRolesActivos) {
			throw new ConflictoException(
					"No se puede desactivar: algún usuario se quedaría sin roles activos. Asígnele otro rol antes.");
		}
	}

}
