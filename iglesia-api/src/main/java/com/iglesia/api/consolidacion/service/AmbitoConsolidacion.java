package com.iglesia.api.consolidacion.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Qué casos y personas puede ver el usuario del token y sobre cuáles puede actuar, calculado por permiso:
 * <ul>
 * <li>{@code consolidacion.administrar} (coordinador): todo, sin límite de ámbito, incluido lo del consolidador;</li>
 * <li>{@code consolidacion.ver}: ver todo, sin actuar;</li>
 * <li>{@code consolidacion.contactar}: ver y actuar en sus casos; ver el historial de las personas que tiene en un
 * caso abierto y de las de su red; abrir seguimientos a personas de su red.</li>
 * </ul>
 * Fuera del ámbito el servicio responde 404, como si no existiera.
 */
@Component
class AmbitoConsolidacion {

	private final UsuarioActual usuarioActual;

	private final MiembrosParaConsolidacion miembros;

	private final CasoConsolidacionRepository casos;

	AmbitoConsolidacion(UsuarioActual usuarioActual, MiembrosParaConsolidacion miembros,
			CasoConsolidacionRepository casos) {
		this.usuarioActual = usuarioActual;
		this.miembros = miembros;
		this.casos = casos;
	}

	boolean esCoordinador() {
		return usuarioActual.tienePermiso(Permisos.CONSOLIDACION_ADMINISTRAR);
	}

	boolean veTodo() {
		return esCoordinador() || usuarioActual.tienePermiso(Permisos.CONSOLIDACION_VER);
	}

	/** Miembro del usuario, solo si puede contactar (sin miembro vinculado no tiene casos propios). */
	Optional<Long> consolidador() {
		boolean contacta = esCoordinador() || usuarioActual.tienePermiso(Permisos.CONSOLIDACION_CONTACTAR);
		return contacta ? usuarioActual.miembroId() : Optional.empty();
	}

	boolean puedeVer(CasoConsolidacion caso) {
		return veTodo() || esPropio(caso) || deSuRed(caso.getMiembroId());
	}

	/** Registrar contactos, cerrar y reabrir: el coordinador en cualquiera; el consolidador, en los suyos. */
	boolean puedeActuar(CasoConsolidacion caso) {
		return esCoordinador() || (usuarioActual.tienePermiso(Permisos.CONSOLIDACION_CONTACTAR) && esPropio(caso));
	}

	/** Historial y resumen de una persona. */
	boolean puedeVerPersona(Long miembroId) {
		return veTodo() || deSuRed(miembroId) || consolidador()
			.map(propio -> casos.existsByConsolidadorIdAndMiembroIdAndEstadoNot(propio, miembroId, EstadoCaso.CERRADO))
			.orElse(false);
	}

	/** Abrir un seguimiento individual: el coordinador a cualquiera; el consolidador, a su red. */
	boolean puedeIniciarSeguimiento(Long miembroId) {
		return esCoordinador() || (usuarioActual.tienePermiso(Permisos.CONSOLIDACION_CONTACTAR) && deSuRed(miembroId));
	}

	private boolean esPropio(CasoConsolidacion caso) {
		return consolidador().filter(propio -> propio.equals(caso.getConsolidadorId())).isPresent();
	}

	private boolean deSuRed(Long miembroId) {
		if (!usuarioActual.tienePermiso(Permisos.CONSOLIDACION_CONTACTAR)) {
			return false;
		}
		return usuarioActual.miembroId().map(miembros::red).map(red -> red.contains(miembroId)).orElse(false);
	}

}
