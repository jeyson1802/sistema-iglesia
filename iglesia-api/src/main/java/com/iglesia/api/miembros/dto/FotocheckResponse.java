package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un miembro para imprimir su fotocheck (credencial con su QR de asistencia).")
public record FotocheckResponse(
		@Schema(description = "Identificador del miembro.", example = "12") Long miembroId,
		@Schema(description = "Nombres.", example = "Ana María") String nombres,
		@Schema(description = "Apellidos.", example = "Torres Ruiz") String apellidos,
		@Schema(description = "Nombre de su nivel, si tiene.", example = "Líder") String nivel,
		@Schema(description = "Código de asistencia (12 caracteres en base32 de Crockford).",
				example = "7K2QM9XDR4TB") String codigo,
		@Schema(description = "Texto que se codifica en el QR.", example = "ASIS:7K2QM9XDR4TB") String contenidoQr) {
}
