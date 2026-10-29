package com.iglesia.api.asistencia.model;

/**
 * A quiénes puede seguir el usuario del token: a toda la iglesia, a la red de un líder (las personas que lidera
 * directa e indirectamente) o a nadie.
 *
 * @param completo si alcanza a toda la iglesia
 * @param liderId raíz de la red cuando no es toda la iglesia; {@code null} si es completo o no tiene alcance
 */
public record AlcanceSeguimiento(boolean completo, Long liderId) {

	public static AlcanceSeguimiento todaLaIglesia() {
		return new AlcanceSeguimiento(true, null);
	}

	public static AlcanceSeguimiento redDe(Long liderId) {
		return new AlcanceSeguimiento(false, liderId);
	}

	public static AlcanceSeguimiento ninguno() {
		return new AlcanceSeguimiento(false, null);
	}

	public boolean vacio() {
		return !completo && liderId == null;
	}

}
