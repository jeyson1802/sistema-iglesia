package com.iglesia.seguridad.auth.component;

import java.time.Duration;

import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.auth.config.CookieRefrescoProperties;
import com.iglesia.seguridad.jwt.config.JwtProperties;

/**
 * Construye la cookie {@code HttpOnly} del token de refresco. {@code Secure} va siempre activo
 * (los navegadores lo aceptan en {@code http://localhost}) para que no exista una bandera que
 * pueda quedar desactivada en producción.
 */
@Component
public class CookieRefresco {

	private final CookieRefrescoProperties propiedades;

	private final Duration duracion;

	CookieRefresco(CookieRefrescoProperties propiedades, JwtProperties jwtProperties) {
		this.propiedades = propiedades;
		this.duracion = jwtProperties.duracionTokenRefresco();
	}

	/** Cookie que entrega al navegador el token de refresco recién emitido. */
	public ResponseCookie emitir(String tokenRefresco) {
		return construir(tokenRefresco, duracion);
	}

	/** Cookie vacía con {@code Max-Age=0} para que el navegador la elimine. */
	public ResponseCookie borrar() {
		return construir("", Duration.ZERO);
	}

	private ResponseCookie construir(String valor, Duration maxAge) {
		return ResponseCookie.from(propiedades.nombre(), valor)
			.path(propiedades.ruta())
			.maxAge(maxAge)
			.httpOnly(true)
			.secure(true)
			.sameSite(SameSite.STRICT.attributeValue())
			.build();
	}

}
