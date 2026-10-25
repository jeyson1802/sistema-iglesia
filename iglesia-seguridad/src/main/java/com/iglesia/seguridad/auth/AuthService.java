package com.iglesia.seguridad.auth;

import java.time.Clock;
import java.time.Duration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenResponse;
import com.iglesia.seguridad.jwt.GeneradorTokenAcceso;
import com.iglesia.seguridad.jwt.TokenAcceso;
import com.iglesia.seguridad.usuario.Correo;
import com.iglesia.seguridad.usuario.Usuario;
import com.iglesia.seguridad.usuario.UsuarioRepository;

/** Inicio de sesión, refresco y cierre de sesión. */
@Service
class AuthService {

	private final AuthenticationManager gestorAutenticacion;

	private final UsuarioRepository usuarios;

	private final GeneradorTokenAcceso generadorTokenAcceso;

	private final TokenRefrescoService tokensRefresco;

	private final Clock reloj;

	AuthService(AuthenticationManager gestorAutenticacion, UsuarioRepository usuarios,
			GeneradorTokenAcceso generadorTokenAcceso, TokenRefrescoService tokensRefresco, Clock reloj) {
		this.gestorAutenticacion = gestorAutenticacion;
		this.usuarios = usuarios;
		this.generadorTokenAcceso = generadorTokenAcceso;
		this.tokensRefresco = tokensRefresco;
		this.reloj = reloj;
	}

	@Transactional
	public TokenResponse iniciarSesion(LoginRequest solicitud) {
		Authentication autenticacion = gestorAutenticacion.authenticate(UsernamePasswordAuthenticationToken
			.unauthenticated(Correo.normalizar(solicitud.correo()), solicitud.contrasena()));
		Usuario usuario = usuarios.findByCorreo(autenticacion.getName())
			.orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
		return emitirTokens(usuario);
	}

	@Transactional(noRollbackFor = TokenRefrescoInvalidoException.class)
	public TokenResponse refrescar(String tokenRefresco) {
		return emitirTokens(tokensRefresco.consumir(tokenRefresco));
	}

	public void cerrarSesion(String tokenRefresco) {
		tokensRefresco.revocar(tokenRefresco);
	}

	private TokenResponse emitirTokens(Usuario usuario) {
		TokenAcceso acceso = generadorTokenAcceso.generar(usuario);
		long segundos = Duration.between(reloj.instant(), acceso.expiraEn()).toSeconds();
		return new TokenResponse(acceso.valor(), TokenResponse.TIPO_BEARER, segundos, tokensRefresco.emitir(usuario));
	}

}
