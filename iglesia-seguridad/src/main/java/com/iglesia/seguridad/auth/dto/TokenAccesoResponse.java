package com.iglesia.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de inicio de sesión y refresco. El token de refresco no viaja aquí: va en una cookie
 * {@code HttpOnly}.
 *
 * @param tokenAcceso JWT para la cabecera {@code Authorization: Bearer}
 * @param tipo        siempre {@code Bearer}
 * @param expiraEn    segundos de vigencia del token de acceso
 */
@Schema(description = "Token de acceso emitido. El token de refresco no viaja en el cuerpo: llega en una cookie HttpOnly.")
public record TokenAccesoResponse(
		@Schema(description = "JWT firmado con RS256 para la cabecera `Authorization: Bearer`. Incluye los claims "
				+ "`sub` (id del usuario), `roles`, `iglesia_id` y `miembro_id`.",
				example = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMiJ9.firma") String tokenAcceso,
		@Schema(description = "Tipo de token; siempre `Bearer`.", example = TIPO_BEARER) String tipo,
		@Schema(description = "Segundos de vigencia del token de acceso.", example = "900") long expiraEn) {

	public static final String TIPO_BEARER = "Bearer";

}
