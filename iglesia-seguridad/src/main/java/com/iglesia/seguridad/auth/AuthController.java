package com.iglesia.seguridad.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;
import com.iglesia.seguridad.auth.dto.TokenRefrescoRequest;
import com.iglesia.seguridad.auth.dto.TokenResponse;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AuthService servicio;

	private final CookieRefresco cookieRefresco;

	AuthController(AuthService servicio, CookieRefresco cookieRefresco) {
		this.servicio = servicio;
		this.cookieRefresco = cookieRefresco;
	}

	@PostMapping("/login")
	ResponseEntity<TokenAccesoResponse> iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		SesionEmitida sesion = servicio.iniciarSesion(solicitud);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieRefresco.emitir(sesion.tokenRefresco()).toString())
			.body(sesion.acceso());
	}

	@PostMapping("/refresh")
	TokenResponse refrescar(@Valid @RequestBody TokenRefrescoRequest solicitud) {
		SesionEmitida sesion = servicio.refrescar(solicitud.tokenRefresco());
		TokenAccesoResponse acceso = sesion.acceso();
		return new TokenResponse(acceso.tokenAcceso(), acceso.tipo(), acceso.expiraEn(), sesion.tokenRefresco());
	}

	@PostMapping("/logout")
	ResponseEntity<Void> cerrarSesion(@Valid @RequestBody TokenRefrescoRequest solicitud) {
		servicio.cerrarSesion(solicitud.tokenRefresco());
		return ResponseEntity.noContent().build();
	}

}
