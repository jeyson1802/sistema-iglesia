package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Código del QR personal de asistencia de un miembro. El frontend genera la imagen del QR y el "
		+ "fotocheck; el backend nunca entrega imágenes.")
public record CodigoAsistenciaResponse(
		@Schema(description = "Código de 12 caracteres (base32 de Crockford), para imprimirlo bajo el QR.",
				example = "7K2QM9XDR4TB") String codigo,
		@Schema(description = "Texto que se codifica en el QR.", example = "ASIS:7K2QM9XDR4TB") String contenidoQr) {
}
