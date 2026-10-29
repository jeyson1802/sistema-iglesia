package com.iglesia.api.iglesia.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import com.iglesia.api.iglesia.model.LogoIglesia;

/**
 * Arma la respuesta de un logo para los dos controladores que lo sirven. El {@code ETag} es el instante en que se
 * subió: el navegador vuelve a preguntar cada vez ({@code no-cache}) y, si no cambió, recibe un 304 sin la imagen. Es
 * {@code private} porque el logo exige autenticación.
 */
final class RespuestaLogo {

	private RespuestaLogo() {
	}

	/** {@code null} cuando el cliente ya tiene esta versión: Spring responde 304. */
	static ResponseEntity<byte[]> de(LogoIglesia logo, WebRequest peticion) {
		String etiqueta = "\"" + logo.actualizadoEn().toEpochMilli() + "\"";
		if (peticion.checkNotModified(etiqueta)) {
			return null;
		}
		return ResponseEntity.ok()
			.eTag(etiqueta)
			.cacheControl(CacheControl.noCache().cachePrivate())
			.contentType(MediaType.parseMediaType(logo.tipo()))
			.body(logo.contenido());
	}

}
