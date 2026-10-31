package com.iglesia.api.common.estadisticas.component;

import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.estadisticas.model.AlcanceEstadisticas;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.RolesBase;

/**
 * Alcance de las estadísticas por iglesia, calculado solo desde el token y por rol base (como el registro de
 * miembros): {@code ADMIN_IGLESIA}, o el administrador del sistema dentro de una iglesia, ve toda la iglesia; un
 * {@code LIDER} vinculado a un miembro, solo su red. Cualquier otro, o fuera de una iglesia, no tiene estadísticas
 * (403). El permiso del módulo lo exige antes el controlador.
 */
@Component
public class ResolutorAlcanceEstadisticas {

	static final String SIN_ALCANCE = "Sin alcance para ver estadísticas";

	private final UsuarioActual usuarioActual;

	private final IglesiaActual iglesiaActual;

	ResolutorAlcanceEstadisticas(UsuarioActual usuarioActual, IglesiaActual iglesiaActual) {
		this.usuarioActual = usuarioActual;
		this.iglesiaActual = iglesiaActual;
	}

	public AlcanceEstadisticas actual() {
		if (iglesiaActual.id().isEmpty()) {
			throw new AccessDeniedException(SIN_ALCANCE);
		}
		Set<String> roles = usuarioActual.rolesBase();
		if (roles.contains(RolesBase.ADMIN_IGLESIA) || roles.contains(RolesBase.ADMIN_SISTEMA)) {
			return AlcanceEstadisticas.iglesia();
		}
		if (roles.contains(RolesBase.LIDER)) {
			return usuarioActual.miembroId()
				.map(AlcanceEstadisticas::redDe)
				.orElseThrow(() -> new AccessDeniedException(SIN_ALCANCE));
		}
		throw new AccessDeniedException(SIN_ALCANCE);
	}

	/** Solo toda la iglesia: para los módulos que un líder no ve en su inicio (consolidación, aportes). */
	public void exigirIglesiaCompleta() {
		if (actual().esRed()) {
			throw new AccessDeniedException(SIN_ALCANCE);
		}
	}

}
