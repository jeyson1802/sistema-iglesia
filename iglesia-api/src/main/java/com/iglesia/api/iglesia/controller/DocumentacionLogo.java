package com.iglesia.api.iglesia.controller;

/** Textos de OpenAPI comunes a los endpoints del logo de los dos controladores. */
final class DocumentacionLogo {

	static final String CUERPO = """
			La imagen como cuerpo binario de la petición (no multipart), con su `Content-Type` (`image/png`, \
			`image/jpeg` o `image/webp`). El tipo real se valida por los primeros bytes del archivo, no por la \
			cabecera; SVG no se admite. Máximo 1 MB.""";

	static final String INVALIDO = "El archivo está vacío o no es una imagen PNG, JPEG o WebP (se valida por su firma).";

	static final String DEMASIADO_GRANDE = "La imagen supera 1 MB.";

	static final String SIN_LOGO = "La iglesia no tiene logo (o no existe).";

	static final String LOGO = "Imagen del logo, con `ETag`; `304` si el `If-None-Match` coincide.";

	private DocumentacionLogo() {
	}

}
