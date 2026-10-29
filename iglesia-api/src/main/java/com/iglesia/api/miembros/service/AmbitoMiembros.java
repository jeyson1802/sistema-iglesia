package com.iglesia.api.miembros.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.RolesBase;
import com.iglesia.api.miembros.model.AmbitoLectura;
import com.iglesia.api.miembros.repository.MiembroRepository;

/**
 * Qué miembros puede leer el usuario del token, calculado solo desde el token:
 * <ul>
 * <li>{@code ADMIN_IGLESIA}, o el administrador del sistema dentro de una iglesia: toda la iglesia;</li>
 * <li>{@code LIDER} vinculado a un miembro: él mismo, su red (directos e indirectos) y su cadena hasta el pastor;</li>
 * <li>cualquier otro: nadie.</li>
 * </ul>
 * Fuera de una iglesia Hibernate ya no devuelve filas, así que no hace falta distinguir ese caso.
 */
@Component
class AmbitoMiembros {

	private final UsuarioActual usuarioActual;

	private final IglesiaActual iglesiaActual;

	private final MiembroRepository miembros;

	AmbitoMiembros(UsuarioActual usuarioActual, IglesiaActual iglesiaActual, MiembroRepository miembros) {
		this.usuarioActual = usuarioActual;
		this.iglesiaActual = iglesiaActual;
		this.miembros = miembros;
	}

	AmbitoLectura actual() {
		Set<String> roles = usuarioActual.rolesBase();
		if (roles.contains(RolesBase.ADMIN_IGLESIA) || roles.contains(RolesBase.ADMIN_SISTEMA)) {
			return AmbitoLectura.completo();
		}
		if (roles.contains(RolesBase.LIDER)) {
			return usuarioActual.miembroId().map(this::deLider).orElseGet(AmbitoLectura::ninguno);
		}
		return AmbitoLectura.ninguno();
	}

	private AmbitoLectura deLider(Long miembroId) {
		var iglesia = iglesiaActual.id();
		if (iglesia.isEmpty() || !miembros.existsById(miembroId)) {
			return AmbitoLectura.ninguno();
		}
		Set<Long> visibles = new HashSet<>();
		visibles.add(miembroId);
		visibles.addAll(miembros.idsDeLaRed(iglesia.get(), miembroId));
		visibles.addAll(miembros.idsDeLaCadena(iglesia.get(), miembroId));
		return AmbitoLectura.soloDe(visibles);
	}

}
