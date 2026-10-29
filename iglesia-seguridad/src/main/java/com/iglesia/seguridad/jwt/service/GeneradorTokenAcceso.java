package com.iglesia.seguridad.jwt.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.iglesia.seguridad.jwt.config.JwtProperties;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;
import com.iglesia.seguridad.jwt.model.TokenAcceso;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.entity.Usuario;

/**
 * Emite el token de acceso (JWT RS256). El {@code sub} es el id del usuario; los datos personales
 * no viajan en el token. Incluye los roles base y los permisos de sus roles activos, la iglesia (tenant)
 * y el miembro vinculado cuando existen, y si la contraseña es temporal. La autorización se decide por permiso,
 * nunca por rol.
 * <p>
 * Cuando el administrador del sistema opera dentro de una iglesia, el token lleva esa iglesia, el claim
 * {@code actuando_en_iglesia} y, sumados a los suyos, los permisos del rol base {@code ADMIN_IGLESIA}.
 */
@Component
public class GeneradorTokenAcceso {

	private final JwtEncoder codificador;

	private final JwtProperties propiedades;

	private final String identificadorClave;

	private final Clock reloj;

	private final RolService servicioRoles;

	GeneradorTokenAcceso(JwtEncoder codificador, JwtProperties propiedades, RSAKey claveRsa, Clock reloj,
			RolService servicioRoles) {
		this.codificador = codificador;
		this.propiedades = propiedades;
		this.identificadorClave = claveRsa.getKeyID();
		this.reloj = reloj;
		this.servicioRoles = servicioRoles;
	}

	/**
	 * @param iglesiaActuandoId iglesia en la que opera el administrador del sistema; {@code null} = la del
	 *                          usuario (o ninguna)
	 */
	public TokenAcceso generar(Usuario usuario, Long iglesiaActuandoId) {
		Instant ahora = reloj.instant();
		Instant expiraEn = ahora.plus(propiedades.duracionTokenAcceso());
		boolean actuando = iglesiaActuandoId != null;
		Long iglesiaId = actuando ? iglesiaActuandoId : usuario.getIglesiaId();
		List<String> roles = usuario.getRolesBaseActivos().stream().map(Enum::name).sorted().toList();
		List<String> permisos = permisos(usuario, actuando);

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.id(UUID.randomUUID().toString())
			.issuer(propiedades.emisor())
			.audience(propiedades.audiencias())
			.subject(usuario.getId().toString())
			.issuedAt(ahora)
			.expiresAt(expiraEn)
			.claim(ClaimsJwt.ROLES, roles)
			.claim(ClaimsJwt.PERMISOS, permisos)
			.claim(ClaimsJwt.CAMBIO_CONTRASENA, usuario.isDebeCambiarContrasena());
		if (iglesiaId != null) {
			claims.claim(ClaimsJwt.IGLESIA_ID, iglesiaId);
		}
		if (actuando) {
			claims.claim(ClaimsJwt.ACTUANDO_EN_IGLESIA, true);
		}
		if (usuario.getMiembroId() != null) {
			claims.claim(ClaimsJwt.MIEMBRO_ID, usuario.getMiembroId());
		}

		JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(identificadorClave).build();
		String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
		return new TokenAcceso(valor, expiraEn);
	}

	/** Permisos de sus roles activos y, si opera dentro de una iglesia, los de un administrador de iglesia. */
	private List<String> permisos(Usuario usuario, boolean actuando) {
		Stream<String> propios = usuario.getPermisosActivos().stream();
		Stream<String> todos = actuando
				? Stream.concat(propios, servicioRoles.permisosDeRolBase(RolBase.ADMIN_IGLESIA).stream()) : propios;
		return todos.distinct().sorted().toList();
	}

}
