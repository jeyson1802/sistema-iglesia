package com.iglesia.seguridad.auth.dto;

/**
 * Respuesta de inicio de sesión y refresco. El token de refresco no viaja aquí: va en una cookie
 * {@code HttpOnly}.
 *
 * @param tokenAcceso JWT para la cabecera {@code Authorization: Bearer}
 * @param tipo        siempre {@code Bearer}
 * @param expiraEn    segundos de vigencia del token de acceso
 */
public record TokenAccesoResponse(String tokenAcceso, String tipo, long expiraEn) {

	public static final String TIPO_BEARER = "Bearer";

}
