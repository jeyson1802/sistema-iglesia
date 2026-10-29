package com.iglesia.api.asistencia.model;

import java.util.Set;

/**
 * Reuniones sobre las que el usuario del token puede actuar (marcar o consultar): todas las de la iglesia, o solo
 * las de los grupos que lidera. Un conjunto vacío sin {@code todas} no deja actuar sobre ninguna.
 */
public record AlcanceAsistencia(boolean todas, Set<Long> grupoIds) {

	public AlcanceAsistencia {
		grupoIds = Set.copyOf(grupoIds);
	}

	public static AlcanceAsistencia completo() {
		return new AlcanceAsistencia(true, Set.of());
	}

	public static AlcanceAsistencia deGrupos(Set<Long> grupoIds) {
		return new AlcanceAsistencia(false, grupoIds);
	}

	public boolean incluye(TipoReunion tipo, Long grupoId) {
		return todas || (tipo == TipoReunion.GRUPO && grupoIds.contains(grupoId));
	}

	public boolean incluyeGrupo(Long grupoId) {
		return todas || grupoIds.contains(grupoId);
	}

	public boolean vacio() {
		return !todas && grupoIds.isEmpty();
	}

}
