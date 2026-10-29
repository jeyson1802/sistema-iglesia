package com.iglesia.api.aportes.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.iglesia.api.aportes.model.AlcanceAportes;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.RolesBase;

/**
 * Qué aportes puede leer el usuario del token, calculado solo desde el token:
 * <ul>
 * <li>{@code ADMIN_IGLESIA}, o el administrador del sistema dentro de una iglesia: todos;</li>
 * <li>cualquier otro usuario vinculado a un miembro (p. ej. {@code LIDER}): solo los suyos;</li>
 * <li>sin miembro vinculado: ninguno.</li>
 * </ul>
 * El permiso {@code aportes.ver} lo exige antes el controlador.
 */
@Component
class AmbitoAportes {

	private final UsuarioActual usuarioActual;

	AmbitoAportes(UsuarioActual usuarioActual) {
		this.usuarioActual = usuarioActual;
	}

	AlcanceAportes actual() {
		Set<String> roles = usuarioActual.rolesBase();
		if (roles.contains(RolesBase.ADMIN_IGLESIA) || roles.contains(RolesBase.ADMIN_SISTEMA)) {
			return AlcanceAportes.completo();
		}
		return usuarioActual.miembroId().map(AlcanceAportes::soloDe).orElseGet(AlcanceAportes::ninguno);
	}

}
