package com.iglesia.api.common.estadisticas.model;

import java.util.Optional;

/**
 * Sobre quiénes se calculan las estadísticas de una página de inicio: toda la iglesia o la red de un líder (las
 * personas que lidera directa e indirectamente, sin él mismo).
 *
 * @param liderId miembro cuya red se cuenta; {@code null} para toda la iglesia
 */
public record AlcanceEstadisticas(Long liderId) {

	public static AlcanceEstadisticas iglesia() {
		return new AlcanceEstadisticas(null);
	}

	public static AlcanceEstadisticas redDe(Long liderId) {
		return new AlcanceEstadisticas(liderId);
	}

	public boolean esRed() {
		return liderId != null;
	}

	public Optional<Long> lider() {
		return Optional.ofNullable(liderId);
	}

}
