package com.iglesia.api;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Ayudas de las pruebas de integración del negocio: iglesias propias de cada prueba y tokens por rol. */
public final class PruebasNegocio {

	public static final String USUARIO = "7";

	private PruebasNegocio() {
	}

	/** Crea una iglesia con nombre único (cada prueba usa la suya para no depender de las demás). */
	public static long nuevaIglesia(JdbcTemplate jdbc) {
		return jdbc.queryForObject("insert into iglesia (nombre) values (?) returning id", Long.class,
				"Prueba " + UUID.randomUUID());
	}

	/** {@code ADMIN_IGLESIA} con todos los permisos de miembros, niveles, grupos y aportes. */
	public static JwtRequestPostProcessor administradorDe(long iglesiaId) {
		return token(iglesiaId, List.of("ADMIN_IGLESIA"), null, Permisos.MIEMBROS_VER, Permisos.MIEMBROS_CREAR,
				Permisos.MIEMBROS_EDITAR, Permisos.MIEMBROS_DESACTIVAR, Permisos.NIVELES_ADMINISTRAR,
				Permisos.GRUPOS_VER, Permisos.GRUPOS_ADMINISTRAR, Permisos.APORTES_VER, Permisos.APORTES_REGISTRAR,
				Permisos.APORTES_EDITAR, Permisos.APORTES_ANULAR, Permisos.APORTES_CONFIGURAR);
	}

	/** {@code LIDER} vinculado a un miembro, que solo tiene {@code miembros.ver}. */
	public static JwtRequestPostProcessor liderDe(long iglesiaId, long miembroId) {
		return token(iglesiaId, List.of("LIDER"), miembroId, Permisos.MIEMBROS_VER);
	}

	public static JwtRequestPostProcessor token(long iglesiaId, List<String> roles, Long miembroId,
			String... permisos) {
		return jwt().jwt(token -> {
			token.subject(USUARIO).claim(ClaimsJwt.IGLESIA_ID, iglesiaId).claim(ClaimsJwt.ROLES, roles);
			if (miembroId != null) {
				token.claim(ClaimsJwt.MIEMBRO_ID, miembroId);
			}
		}).authorities(Arrays.stream(permisos).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
	}

	public static long idDe(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
