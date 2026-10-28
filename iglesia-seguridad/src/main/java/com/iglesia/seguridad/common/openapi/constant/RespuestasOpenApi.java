package com.iglesia.seguridad.common.openapi.constant;

/** Descripciones de las respuestas de error que se repiten en muchos endpoints (para usar en {@code @ApiResponse}). */
public final class RespuestasOpenApi {

	public static final String DATOS_INVALIDOS = "Datos inválidos: algún campo no cumple las validaciones. "
			+ "La propiedad `errores` indica el mensaje de cada campo.";

	public static final String NO_AUTENTICADO = "No autenticado: falta el token de acceso, es inválido o expiró. "
			+ "Renovarlo con `POST /api/auth/refresh`.";

	public static final String SIN_PERMISO = "Acceso denegado: el usuario autenticado no tiene el permiso requerido "
			+ "o tiene una contraseña temporal que debe cambiar antes de continuar.";

	private RespuestasOpenApi() {
	}

}
