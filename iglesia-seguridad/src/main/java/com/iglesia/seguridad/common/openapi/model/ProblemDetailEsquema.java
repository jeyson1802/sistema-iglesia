package com.iglesia.seguridad.common.openapi.model;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Forma real en JSON de los errores {@code ProblemDetail} que devuelve la API. Solo se usa para documentar:
 * la clase de Spring agrupa {@code traceId} y {@code errores} en un mapa que Jackson aplana al serializar.
 */
@Schema(name = "ProblemDetail", description = "Error en formato ProblemDetail (RFC 9457), con mensajes en español. "
		+ "Se devuelve con el tipo de contenido application/problem+json.")
public record ProblemDetailEsquema(
		@Schema(description = "URI que identifica el tipo de problema.", example = "about:blank",
				format = "uri") String type,
		@Schema(description = "Resumen corto del problema.", example = "Recurso no encontrado") String title,
		@Schema(description = "Código de estado HTTP.", example = "404") int status,
		@Schema(description = "Explicación del problema para el usuario.",
				example = "Usuario no encontrado") String detail,
		@Schema(description = "Ruta de la petición que falló.", example = "/api/usuarios/99",
				format = "uri") String instance,
		@Schema(description = "Código de seguimiento de la petición (igual a la cabecera X-Trace-Id). "
				+ "Comunicarlo a soporte para rastrear el error en los logs.",
				example = "4bf92f3577b34da6a3ce929d0e0e4736") String traceId,
		@Schema(description = "Solo en los 400 de validación: mensaje de error por campo.",
				example = "{\"correo\": \"debe ser una dirección de correo electrónico con formato correcto\"}",
				nullable = true) Map<String, String> errores) {
}
