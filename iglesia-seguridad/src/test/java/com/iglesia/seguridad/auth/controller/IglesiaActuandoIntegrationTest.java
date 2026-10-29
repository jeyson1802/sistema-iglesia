package com.iglesia.seguridad.auth.controller;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrar y salir de una iglesia como administrador del sistema: la iglesia se guarda con el token de refresco y el
 * refresco la conserva.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class IglesiaActuandoIntegrationTest {

	private static final String COOKIE_REFRESCO = "iglesia_refresco";

	private static final String RUTA = "/api/auth/iglesia-actuando";

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
	void entraAUnaIglesiaRefrescaYSaleRotandoLaCookie(CapturedOutput salida) throws Exception {
		long usuarioId = semilla.crearUsuario("sistema@actuando4.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		MvcResult login = iniciarSesion("sistema@actuando4.test");
		String acceso = tokenDe(login);
		String refrescoInicial = cookieDe(login);

		MvcResult entrada = entrar(acceso, refrescoInicial, "{\"iglesiaId\":74}").andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(COOKIE_REFRESCO + "=")))
			.andReturn();
		Jwt dentro = accesoDe(entrada);
		assertThat(dentro.<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(74L);
		assertThat(dentro.<Object>getClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isEqualTo(true);
		assertThat(dentro.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_SISTEMA");
		assertThat(dentro.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactlyInAnyOrder("iglesias.administrar",
				"iglesias.ver", "miembros.crear", "miembros.desactivar", "miembros.editar", "miembros.ver",
				"niveles.administrar", "grupos.ver", "grupos.administrar", "aportes.ver", "aportes.registrar",
				"aportes.editar", "aportes.anular", "aportes.configurar", "roles.administrar", "roles.ver",
				"usuarios.administrar", "usuarios.ver");

		MvcResult refrescado = refrescar(cookieDe(entrada));
		assertThat(accesoDe(refrescado).<Long>getClaim(ClaimsJwt.IGLESIA_ID)).isEqualTo(74L);

		MvcResult salida1 = salir(tokenDe(refrescado), cookieDe(refrescado)).andExpect(status().isOk()).andReturn();
		Jwt fuera = accesoDe(salida1);
		assertThat(fuera.hasClaim(ClaimsJwt.IGLESIA_ID)).isFalse();
		assertThat(fuera.hasClaim(ClaimsJwt.ACTUANDO_EN_IGLESIA)).isFalse();
		assertThat(fuera.getClaimAsStringList(ClaimsJwt.PERMISOS)).doesNotContain("roles.administrar");
		assertThat(accesoDe(refrescar(cookieDe(salida1))).hasClaim(ClaimsJwt.IGLESIA_ID)).isFalse();

		// La cookie usada al entrar quedó revocada.
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE_REFRESCO, refrescoInicial)))
			.andExpect(status().isUnauthorized());
		assertThat(salida.getOut())
			.contains("Administrador del sistema usuarioId=" + usuarioId + " entra a iglesiaId=74")
			.contains("Administrador del sistema usuarioId=" + usuarioId + " sale de iglesiaId=74")
			.doesNotContain("sistema@actuando4.test");
	}

	@Test
	void sinCookieOConLaCookieDeOtroUsuarioRespondeNoAutenticadoYBorraLaCookie() throws Exception {
		semilla.crearUsuario("sistema@actuando5.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		semilla.crearUsuario("otro@actuando5.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		String acceso = tokenDe(iniciarSesion("sistema@actuando5.test"));
		String cookieAjena = cookieDe(iniciarSesion("otro@actuando5.test"));

		mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"iglesiaId\":75}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		mvc.perform(delete(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso))
			.andExpect(status().isUnauthorized());
		entrar(acceso, cookieAjena, "{\"iglesiaId\":75}").andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
	}

	@Test
	void sinTokenDeAccesoSinPermisoOConDatosInvalidosSeRechaza() throws Exception {
		semilla.crearUsuario("sistema@actuando6.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		semilla.crearUsuario("admin@actuando6.test", 76L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		MvcResult sistema = iniciarSesion("sistema@actuando6.test");
		MvcResult admin = iniciarSesion("admin@actuando6.test");

		mvc.perform(post(RUTA).cookie(new Cookie(COOKIE_REFRESCO, cookieDe(sistema)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"iglesiaId\":76}"))
			.andExpect(status().isUnauthorized());
		entrar(tokenDe(admin), cookieDe(admin), "{\"iglesiaId\":77}").andExpect(status().isForbidden())
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		salir(tokenDe(admin), cookieDe(admin)).andExpect(status().isForbidden());
		entrar(tokenDe(sistema), cookieDe(sistema), "{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.iglesiaId").exists());
		entrar(tokenDe(sistema), cookieDe(sistema), "{\"iglesiaId\":0}").andExpect(status().isBadRequest());
	}

	@Test
	void dentroDeUnaIglesiaSoloSeVeYAdministraEsaIglesia() throws Exception {
		long adminIglesia = semilla.crearUsuario("admin@iglesia78.test", 78L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		long deOtraIglesia = semilla.crearUsuario("admin@iglesia79.test", 79L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		long otroSistema = semilla.crearUsuario("otro@actuando7.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		semilla.crearUsuario("sistema@actuando7.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		MvcResult login = iniciarSesion("sistema@actuando7.test");
		String dentro = tokenDe(entrar(tokenDe(login), cookieDe(login), "{\"iglesiaId\":78}").andReturn());

		mvc.perform(get("/api/usuarios").param("iglesiaId", "79").header(HttpHeaders.AUTHORIZATION, bearer(dentro)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].iglesiaId", everyItem(is(78))))
			.andExpect(jsonPath("$.content[*].id", hasItem((int) adminIglesia)));
		mvc.perform(get("/api/usuarios/{id}", deOtraIglesia).header(HttpHeaders.AUTHORIZATION, bearer(dentro)))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/usuarios/{id}", otroSistema).header(HttpHeaders.AUTHORIZATION, bearer(dentro)))
			.andExpect(status().isNotFound());
		mvc.perform(put("/api/usuarios/{id}", otroSistema).header(HttpHeaders.AUTHORIZATION, bearer(dentro))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"otro@actuando7.test\",\"rolIds\":[%d],\"activo\":false}"
				.formatted(semilla.idRolBase("ADMIN_SISTEMA"))))
			.andExpect(status().isNotFound());

		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(dentro)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.iglesiaId").value(78))
			.andExpect(jsonPath("$.actuandoEnIglesia").value(true))
			.andExpect(jsonPath("$.perfiles[0].rolBase").value("ADMIN_SISTEMA"))
			.andExpect(jsonPath("$.perfiles[0].permisos", hasItems("iglesias.administrar", "roles.administrar")));

		String rol = mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(dentro))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Secretaría\",\"rolBaseId\":%d,\"permisos\":[\"miembros.ver\"]}"
				.formatted(semilla.idRolBase("ADMIN_IGLESIA"))))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		long rolId = ((Number) JsonPath.read(rol, "$.id")).longValue();
		assertThat(jdbc.queryForObject("select iglesia_id from rol where id = ?", Long.class, rolId)).isEqualTo(78L);

		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(dentro))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"secretaria@iglesia78.test\",\"contrasena\":\"Temporal123\",\"iglesiaId\":78,"
					+ "\"rolIds\":[%d]}".formatted(rolId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.iglesiaId").value(78));
		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(dentro))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"intruso@iglesia79.test\",\"contrasena\":\"Temporal123\",\"iglesiaId\":79,"
					+ "\"rolIds\":[%d]}".formatted(semilla.idRolBase("ADMIN_IGLESIA"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void dentroDeUnaIglesiaElAdministradorDelSistemaNoCuentaComoSuAdministrador() throws Exception {
		long unico = semilla.crearUsuario("admin@iglesia80.test", 80L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.crearUsuario("sistema@actuando8.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		MvcResult login = iniciarSesion("sistema@actuando8.test");
		String dentro = tokenDe(entrar(tokenDe(login), cookieDe(login), "{\"iglesiaId\":80}").andReturn());

		mvc.perform(put("/api/usuarios/{id}", unico).header(HttpHeaders.AUTHORIZATION, bearer(dentro))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"admin@iglesia80.test\",\"rolIds\":[%d],\"activo\":false}"
				.formatted(semilla.idRolBase("ADMIN_IGLESIA"))))
			.andExpect(status().isConflict());
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

	private ResultActions entrar(String acceso, String refresco, String cuerpo) throws Exception {
		return mvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.cookie(new Cookie(COOKIE_REFRESCO, refresco))
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo));
	}

	private ResultActions salir(String acceso, String refresco) throws Exception {
		return mvc.perform(delete(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.cookie(new Cookie(COOKIE_REFRESCO, refresco)));
	}

	private MvcResult refrescar(String refresco) throws Exception {
		return mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE_REFRESCO, refresco)))
			.andExpect(status().isOk())
			.andReturn();
	}

	private static String cookieDe(MvcResult resultado) {
		return resultado.getResponse().getCookie(COOKIE_REFRESCO).getValue();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private static String tokenDe(MvcResult resultado) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.tokenAcceso");
	}

	private Jwt accesoDe(MvcResult resultado) throws Exception {
		return decodificador.decode(tokenDe(resultado));
	}

}
