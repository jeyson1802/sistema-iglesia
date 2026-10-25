package com.iglesia.seguridad.auth;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenRefrescoRequest;
import com.iglesia.seguridad.auth.dto.TokenResponse;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AuthService servicio;

	AuthController(AuthService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/login")
	TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		return servicio.iniciarSesion(solicitud);
	}

	@PostMapping("/refresh")
	TokenResponse refrescar(@Valid @RequestBody TokenRefrescoRequest solicitud) {
		return servicio.refrescar(solicitud.tokenRefresco());
	}

	@PostMapping("/logout")
	ResponseEntity<Void> cerrarSesion(@Valid @RequestBody TokenRefrescoRequest solicitud) {
		servicio.cerrarSesion(solicitud.tokenRefresco());
		return ResponseEntity.noContent().build();
	}

}
