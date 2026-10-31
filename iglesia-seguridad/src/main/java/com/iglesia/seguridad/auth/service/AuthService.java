package com.iglesia.seguridad.auth.service;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.actividad.service.RegistroActividadService;
import com.iglesia.seguridad.auth.dto.LoginRequest;
import com.iglesia.seguridad.auth.dto.TokenAccesoResponse;
import com.iglesia.seguridad.auth.exception.TokenRefrescoInvalidoException;
import com.iglesia.seguridad.auth.model.RefrescoConsumido;
import com.iglesia.seguridad.auth.model.SesionEmitida;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.jwt.model.TokenAcceso;
import com.iglesia.seguridad.jwt.service.GeneradorTokenAcceso;
import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.model.Correo;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

/** Inicio de sesión, refresco, cierre de sesión y entrada o salida del administrador del sistema a una iglesia. */
@Service
public class AuthService {

	private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

	/** Valor de {@code usuarioId} en el log cuando el correo del login fallido no existe. */
	static final String USUARIO_INEXISTENTE = "inexistente";

	private final AuthenticationManager gestorAutenticacion;

	private final UsuarioRepository usuarios;

	private final GeneradorTokenAcceso generadorTokenAcceso;

	private final TokenRefrescoService tokensRefresco;

	private final ProveedorUsuarioAutenticado proveedorUsuario;

	private final RegistroActividadService actividad;

	private final Clock reloj;

	AuthService(AuthenticationManager gestorAutenticacion, UsuarioRepository usuarios,
			GeneradorTokenAcceso generadorTokenAcceso, TokenRefrescoService tokensRefresco,
			ProveedorUsuarioAutenticado proveedorUsuario, RegistroActividadService actividad, Clock reloj) {
		this.gestorAutenticacion = gestorAutenticacion;
		this.usuarios = usuarios;
		this.generadorTokenAcceso = generadorTokenAcceso;
		this.tokensRefresco = tokensRefresco;
		this.proveedorUsuario = proveedorUsuario;
		this.actividad = actividad;
		this.reloj = reloj;
	}

	/** @param ip dirección del cliente, solo para el log de intentos fallidos */
	@Transactional
	public SesionEmitida iniciarSesion(LoginRequest solicitud, String ip) {
		Authentication autenticacion = autenticar(Correo.normalizar(solicitud.correo()), solicitud.contrasena(), ip);
		Usuario usuario = usuarios.findByCorreo(autenticacion.getName())
			.orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
		SesionEmitida sesion = emitirTokens(usuario, null);
		actividad.registrar(usuario);
		LOG.info("Inicio de sesión exitoso usuarioId={}", usuario.getId());
		return sesion;
	}

	/**
	 * Rota el token de refresco y conserva la iglesia en la que operaba el administrador del sistema, salvo que
	 * ya no pueda entrar a iglesias (perdió {@code iglesias.administrar}): entonces vuelve a la vista global.
	 */
	@Transactional(noRollbackFor = TokenRefrescoInvalidoException.class)
	public SesionEmitida refrescar(String tokenRefresco) {
		RefrescoConsumido consumido = tokensRefresco.consumir(tokenRefresco);
		Usuario usuario = consumido.usuario();
		Long iglesiaActuandoId = puedeEntrarAIglesias(usuario) ? consumido.iglesiaActuandoId() : null;
		SesionEmitida sesion = emitirTokens(usuario, iglesiaActuandoId);
		actividad.registrar(usuario);
		LOG.info("Token renovado usuarioId={}", usuario.getId());
		return sesion;
	}

	public void cerrarSesion(String tokenRefresco) {
		tokensRefresco.revocar(tokenRefresco);
	}

	/**
	 * El administrador del sistema pasa a operar dentro de la iglesia: rota el token de refresco (que la recuerda)
	 * y emite un token de acceso con esa iglesia. La existencia de la iglesia no se valida aquí: el catálogo vive
	 * en iglesia-api.
	 * @throws TokenRefrescoInvalidoException si el token de refresco no es utilizable o es de otro usuario
	 */
	@Transactional(noRollbackFor = TokenRefrescoInvalidoException.class)
	public SesionEmitida entrarAIglesia(String tokenRefresco, Long iglesiaId) {
		Usuario usuario = consumirDelUsuarioActual(tokenRefresco).usuario();
		SesionEmitida sesion = emitirTokens(usuario, iglesiaId);
		LOG.info("Administrador del sistema usuarioId={} entra a iglesiaId={}", usuario.getId(), iglesiaId);
		return sesion;
	}

	/**
	 * El administrador del sistema vuelve a la vista global: rota el token de refresco sin iglesia.
	 * @throws TokenRefrescoInvalidoException si el token de refresco no es utilizable o es de otro usuario
	 */
	@Transactional(noRollbackFor = TokenRefrescoInvalidoException.class)
	public SesionEmitida salirDeIglesia(String tokenRefresco) {
		RefrescoConsumido consumido = consumirDelUsuarioActual(tokenRefresco);
		SesionEmitida sesion = emitirTokens(consumido.usuario(), null);
		LOG.info("Administrador del sistema usuarioId={} sale de iglesiaId={}", consumido.usuario().getId(),
				consumido.iglesiaActuandoId());
		return sesion;
	}

	/** Registra los intentos fallidos con el id del usuario (nunca el correo ni la contraseña) y relanza. */
	private Authentication autenticar(String correo, String contrasena, String ip) {
		try {
			return gestorAutenticacion
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(correo, contrasena));
		}
		catch (DisabledException ex) {
			LOG.info("Inicio de sesión rechazado: usuario inactivo usuarioId={} ip={}", idParaLog(correo), ip);
			throw ex;
		}
		catch (AuthenticationException ex) {
			LOG.info("Inicio de sesión fallido usuarioId={} ip={}", idParaLog(correo), ip);
			throw ex;
		}
	}

	private String idParaLog(String correo) {
		return usuarios.findByCorreo(correo).map(usuario -> usuario.getId().toString()).orElse(USUARIO_INEXISTENTE);
	}

	/** El token de refresco de la cookie debe ser del mismo usuario que el token de acceso. */
	private RefrescoConsumido consumirDelUsuarioActual(String tokenRefresco) {
		RefrescoConsumido consumido = tokensRefresco.consumir(tokenRefresco);
		if (!consumido.usuario().getId().equals(proveedorUsuario.actual().usuarioId())) {
			throw new TokenRefrescoInvalidoException();
		}
		return consumido;
	}

	private static boolean puedeEntrarAIglesias(Usuario usuario) {
		return usuario.getPermisosActivos().contains(Permisos.IGLESIAS_ADMINISTRAR);
	}

	private SesionEmitida emitirTokens(Usuario usuario, Long iglesiaActuandoId) {
		TokenAcceso acceso = generadorTokenAcceso.generar(usuario, iglesiaActuandoId);
		long segundos = Duration.between(reloj.instant(), acceso.expiraEn()).toSeconds();
		return new SesionEmitida(new TokenAccesoResponse(acceso.valor(), TokenAccesoResponse.TIPO_BEARER, segundos),
				tokensRefresco.emitir(usuario, iglesiaActuandoId));
	}

}
