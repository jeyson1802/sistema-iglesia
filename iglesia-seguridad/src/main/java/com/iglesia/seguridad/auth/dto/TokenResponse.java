package com.iglesia.seguridad.auth.dto;

/**
 * @param tokenAcceso    JWT para la cabecera {@code Authorization: Bearer}
 * @param tipo           siempre {@code Bearer}
 * @param expiraEn       segundos de vigencia del token de acceso
 * @param tokenRefresco  token opaco para obtener un nuevo par de tokens
 */
public record TokenResponse(String tokenAcceso, String tipo, long expiraEn, String tokenRefresco) {

	public static final String TIPO_BEARER = "Bearer";

}
