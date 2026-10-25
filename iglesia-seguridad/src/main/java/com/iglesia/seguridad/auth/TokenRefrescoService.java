package com.iglesia.seguridad.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.jwt.JwtProperties;
import com.iglesia.seguridad.usuario.Usuario;

/**
 * Tokens de refresco opacos con rotación: cada uso revoca el token y emite otro. Si se presenta un
 * token ya revocado se asume robo y se revocan todas las sesiones del usuario.
 */
@Service
public class TokenRefrescoService {

	private static final Logger LOG = LoggerFactory.getLogger(TokenRefrescoService.class);

	private static final int BYTES_TOKEN = 32;

	private static final String ALGORITMO_HASH = "SHA-256";

	private final TokenRefrescoRepository tokens;

	private final JwtProperties propiedades;

	private final Clock reloj;

	private final SecureRandom aleatorio = new SecureRandom();

	TokenRefrescoService(TokenRefrescoRepository tokens, JwtProperties propiedades, Clock reloj) {
		this.tokens = tokens;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	/** Emite un token nuevo y devuelve su valor en claro (la única vez que existe). */
	@Transactional
	public String emitir(Usuario usuario) {
		byte[] bytes = new byte[BYTES_TOKEN];
		aleatorio.nextBytes(bytes);
		String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant ahora = reloj.instant();
		tokens.save(new TokenRefresco(usuario, hash(valor), ahora, ahora.plus(propiedades.duracionTokenRefresco())));
		return valor;
	}

	/**
	 * Valida y consume el token (queda revocado).
	 * @return el usuario dueño del token
	 * @throws TokenRefrescoInvalidoException si no es utilizable
	 */
	@Transactional(noRollbackFor = TokenRefrescoInvalidoException.class)
	public Usuario consumir(String valor) {
		Instant ahora = reloj.instant();
		TokenRefresco token = tokens.findByHashToken(hash(valor)).orElseThrow(TokenRefrescoInvalidoException::new);
		Usuario usuario = token.getUsuario();
		if (token.estaRevocado()) {
			LOG.warn("Reutilización de token de refresco revocado; se revocan las sesiones del usuario {}",
					usuario.getId());
			tokens.revocarVigentesDelUsuario(usuario.getId(), ahora);
			throw new TokenRefrescoInvalidoException();
		}
		if (!token.estaVigente(ahora) || !usuario.isActivo()) {
			throw new TokenRefrescoInvalidoException();
		}
		token.revocar(ahora);
		return usuario;
	}

	/** Revoca el token si existe. Es idempotente para que cerrar sesión nunca falle. */
	@Transactional
	public void revocar(String valor) {
		tokens.findByHashToken(hash(valor)).ifPresent(token -> token.revocar(reloj.instant()));
	}

	@Transactional
	public void revocarTodos(Long usuarioId) {
		tokens.revocarVigentesDelUsuario(usuarioId, reloj.instant());
	}

	private static String hash(String valor) {
		try {
			byte[] resumen = MessageDigest.getInstance(ALGORITMO_HASH).digest(valor.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("La JVM no soporta " + ALGORITMO_HASH, ex);
		}
	}

}
