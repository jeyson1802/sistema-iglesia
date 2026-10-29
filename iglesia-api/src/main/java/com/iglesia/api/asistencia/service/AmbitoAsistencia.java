package com.iglesia.api.asistencia.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.AlcanceSeguimiento;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.service.AgendaGrupos;

/**
 * Sobre qué reuniones puede actuar el usuario del token, calculado por permiso (nunca por rol):
 * <ul>
 * <li>marcar: con {@code asistencia.registrar}, todas; con {@code asistencia.grupo}, las de los grupos que lidera;</li>
 * <li>consultar el historial: con {@code asistencia.ver}, todas; con {@code asistencia.grupo}, las de sus grupos;</li>
 * <li>ver una reunión y sus asistentes: lo que pueda marcar o consultar;</li>
 * <li>seguimiento: con {@code asistencia.ver}, toda la iglesia; con {@code asistencia.grupo}, su red.</li>
 * </ul>
 * El controlador ya exigió alguno de los permisos; aquí se decide el alcance.
 */
@Component
class AmbitoAsistencia {

	private final UsuarioActual usuarioActual;

	private final AgendaGrupos grupos;

	AmbitoAsistencia(UsuarioActual usuarioActual, AgendaGrupos grupos) {
		this.usuarioActual = usuarioActual;
		this.grupos = grupos;
	}

	AlcanceAsistencia paraMarcar() {
		return usuarioActual.tienePermiso(Permisos.ASISTENCIA_REGISTRAR) ? AlcanceAsistencia.completo()
				: deSusGrupos();
	}

	AlcanceAsistencia paraConsultar() {
		return usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER) ? AlcanceAsistencia.completo() : deSusGrupos();
	}

	AlcanceAsistencia paraVerReunion() {
		return usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)
				|| usuarioActual.tienePermiso(Permisos.ASISTENCIA_REGISTRAR) ? AlcanceAsistencia.completo()
						: deSusGrupos();
	}

	AlcanceSeguimiento paraSeguimiento() {
		if (usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)) {
			return AlcanceSeguimiento.todaLaIglesia();
		}
		if (!usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)) {
			return AlcanceSeguimiento.ninguno();
		}
		return usuarioActual.miembroId().map(AlcanceSeguimiento::redDe).orElseGet(AlcanceSeguimiento::ninguno);
	}

	/** Grupos que lidera el miembro vinculado al usuario, si tiene {@code asistencia.grupo}; si no, ninguno. */
	private AlcanceAsistencia deSusGrupos() {
		if (!usuarioActual.tienePermiso(Permisos.ASISTENCIA_GRUPO)) {
			return AlcanceAsistencia.deGrupos(Set.of());
		}
		return AlcanceAsistencia.deGrupos(usuarioActual.miembroId().map(grupos::lideradosPor).orElse(Set.of()));
	}

}
