package com.iglesia.seguridad.auth.controller;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Iglesia en la que opera el administrador del sistema: se guarda con el token de refresco y el refresco la conserva. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class IglesiaActuandoIntegrationTest {

	private static final String COOKIE_REFRESCO = "iglesia_refresco";

	private static final String FIJAR_IGLESIA_ACTUANDO = "update token_refresco set iglesia_actuando_id = ? "
			+ "where usuario_id = ? and revocado_en is null";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JwtDecoder decodificador;

	private SemillaSeguridad semilla;

	@BeforeEach
	void configurar() {
		semilla = new SemillaSeguridad(jdbc, codificador);
	}

	@Test
	void elRefrescoConservaLaIglesiaEnLaQueOperaElAdministradorDelSistema() throws Exception {
		long usuarioId = semilla.crearUsuario("sistema@actuando1.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		String refresco = cookieDe(iniciarSesion("sistema@actuando1.test"));
		jdbc.update(FIJAR_IGLESIA_ACTUANDO, 71L, usuarioId);

		MvcResult primero = refrescar(refresco);
		Jwt jwt = accesoDe(primero);

		assertThat(jwt.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(71L);
		assertThat(jwt.<Object>getClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isEqualTo(true);
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_SISTEMA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).contains("iglesias.administrar", "iglesias.ver",
				"miembros.ver", "roles.administrar", "usuarios.administrar");

		Jwt segundo = accesoDe(refrescar(cookieDe(primero)));
		assertThat(segundo.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(71L);
		assertThat(segundo.<Object>getClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isEqualTo(true);
	}

	@Test
	void sinIglesiaDeActuacionElRefrescoSigueEnLaVistaGlobal() throws Exception {
		semilla.crearUsuario("sistema@actuando2.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));

		Jwt jwt = accesoDe(refrescar(cookieDe(iniciarSesion("sistema@actuando2.test"))));

		assertThat(jwt.hasClaim(ClaimsJwt.IGLESIA_ID)).isFalse();
		assertThat(jwt.hasClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isFalse();
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).doesNotContain("roles.administrar");
	}

	@Test
	void quienNoPuedeEntrarAIglesiasNuncaRecibeOtraIglesiaAlRefrescar() throws Exception {
		long usuarioId = semilla.crearUsuario("admin@actuando3.test", 73L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		String refresco = cookieDe(iniciarSesion("admin@actuando3.test"));
		jdbc.update(FIJAR_IGLESIA_ACTUANDO, 99L, usuarioId);

		Jwt jwt = accesoDe(refrescar(refresco));

		assertThat(jwt.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(73L);
		assertThat(jwt.hasClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isFalse();
		assertThat(jdbc.queryForObject("select count(*) from token_refresco where usuario_id = ? "
				+ "and revocado_en is null and iglesia_actuando_id is not null", Long.class, usuarioId)).isZero();
	}

	private MvcResult iniciarSesion(String correo) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, SemillaSeguridad.CONTRASENA)))
			.andExpect(status().isOk())
			.andReturn();
	}

	private MvcResult refrescar(String refresco) throws Exception {
		return mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE_REFRESCO, refresco)))
			.andExpect(status().isOk())
			.andReturn();
	}

	private static String cookieDe(MvcResult resultado) {
		return resultado.getResponse().getCookie(COOKIE_REFRESCO).getValue();
	}

	private Jwt accesoDe(MvcResult resultado) throws Exception {
		return decodificador.decode(JsonPath.read(resultado.getResponse().getContentAsString(), "$.tokenAcceso"));
	}

}
