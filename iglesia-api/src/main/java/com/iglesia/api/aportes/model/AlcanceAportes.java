package com.iglesia.api.aportes.model;

/**
 * Qué aportes puede leer el usuario del token: toda la iglesia, solo los de un miembro (los suyos) o ninguno.
 *
 * @param todaLaIglesia ve todos los aportes de la iglesia
 * @param miembroId si no ve toda la iglesia, el único aportante cuyos aportes ve; {@code null} = ninguno
 */
public record AlcanceAportes(boolean todaLaIglesia, Long miembroId) {

	public static AlcanceAportes completo() {
		return new AlcanceAportes(true, null);
	}

	public static AlcanceAportes soloDe(Long miembroId) {
		return new AlcanceAportes(false, miembroId);
	}

	public static AlcanceAportes ninguno() {
		return new AlcanceAportes(false, null);
	}

	public boolean vacio() {
		return !todaLaIglesia && miembroId == null;
	}

	/** Aportante por el que se filtra: el pedido si ve toda la iglesia; si no, siempre el suyo. */
	public Long aportanteEfectivo(Long pedido) {
		return todaLaIglesia ? pedido : miembroId;
	}

	public boolean incluye(Long aportante) {
		return todaLaIglesia || (miembroId != null && miembroId.equals(aportante));
	}

}
