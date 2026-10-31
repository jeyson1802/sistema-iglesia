package com.iglesia.seguridad.actividad.controller;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actividad diaria (una fila por usuario y día) y cifras de uso según el alcance del token. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasUsoIntegrationTest {

	private static final String COOKIE_REFRESCO = "iglesia_refresco";

	private static final String RUTA = "/api/estadisticas/uso";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder codificador;

	private SemillaSeguridad semilla;

	@BeforeEach
	void configurar() {
		semilla = new SemillaSeguridad(jdbc, codificador);
	}

	@Test
	void iniciarSesionYRenovarDejanUnaSolaFilaPorUsuarioYDia() throws Exception {
		long usuarioId = semilla.crearUsuario("admin@uso911.test", 911L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		iniciarSesion("admin@uso911.test");
		MvcResult segundo = iniciarSesion("admin@uso911.test");
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE_REFRESCO, cookieDe(segundo))))
			.andExpect(status().isOk());

		assertThat(jdbc.queryForList("select iglesia_id, rol_base from actividad_usuario where usuario_id = ?",
				usuarioId))
			.singleElement()
			.satisfies(fila -> {
				assertThat(fila.get("iglesia_id")).isEqualTo(911L);
				assertThat(fila.get("rol_base")).isEqualTo("ADMIN_IGLESIA");
			});
	}

	@Test
	void elAdministradorDeIglesiaSoloVeElUsoDeSuIglesia() throws Exception {
		semilla.crearUsuario("admin@uso912.test", 912L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		long lider = semilla.crearUsuario("lider@uso912.test", 912L, 5L, semilla.idRolBase("LIDER"));
		long deOtra = semilla.crearUsuario("admin@uso913.test", 913L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		LocalDate hoy = LocalDate.now();
		actividad(lider, hoy.minusDays(1), 912L, "LIDER");
		actividad(deOtra, hoy.minusDays(1), 913L, "ADMIN_IGLESIA");
		String acceso = tokenDe(iniciarSesion("admin@uso912.test"));

		mvc.perform(get(RUTA).param("periodo", "PERSONALIZADO")
				.param("desde", hoy.withDayOfMonth(1).minusMonths(11).toString()).param("hasta", hoy.toString()).header(HttpHeaders.AUTHORIZATION, bearer(acceso)))
			.andExpect(status().isOk())
			.andExpect(header().exists("X-Trace-Id"))
			.andExpect(jsonPath("$.global", is(false)))
			.andExpect(jsonPath("$.usuarios", is(2)))
			.andExpect(jsonPath("$.activosMensuales", is(2)))
			.andExpect(jsonPath("$.activosSemanales", is(2)))
			.andExpect(jsonPath("$.iglesiasActivas", nullValue()))
			.andExpect(jsonPath("$.serie", hasSize(12)))
			.andExpect(jsonPath("$.porRolBase", hasSize(3)))
			.andExpect(jsonPath("$.porRolBase[?(@.clave == 'LIDER')].valor").value(1))
			.andExpect(jsonPath("$.periodo.granularidad", is("MES")));
	}

	@Test
	void sinPermisoDeUsuariosNiDelSistemaResponde403() throws Exception {
		semilla.crearUsuario("lider@uso914.test", 914L, 7L, semilla.idRolBase("LIDER"));
		String acceso = tokenDe(iniciarSesion("lider@uso914.test"));

		mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(acceso)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.traceId", notNullValue()));
	}

	@Test
	void elAdministradorDelSistemaVeTodasLasIglesiasEnLaVistaGlobal() throws Exception {
		long otro = semilla.crearUsuario("admin@uso915.test", 915L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		actividad(otro, LocalDate.now(), 915L, "ADMIN_IGLESIA");
		semilla.crearUsuario("sistema@uso915.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		String acceso = tokenDe(iniciarSesion("sistema@uso915.test"));

		String cuerpo = mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(acceso)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.global", is(true)))
			.andExpect(jsonPath("$.porRolBase", hasSize(4)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(JsonPath.<Integer>read(cuerpo, "$.iglesiasActivas")).isGreaterThanOrEqualTo(1);
		assertThat(JsonPath.<Integer>read(cuerpo, "$.activosPeriodo")).isGreaterThanOrEqualTo(2);
		assertThat(cuerpo).doesNotContain("@uso915.test");
	}

	@Test
	void dentroDeUnaIglesiaElAdministradorDelSistemaSoloVeEsaIglesia() throws Exception {
		semilla.crearUsuario("admin@uso916.test", 916L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.crearUsuario("sistema@uso916.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		MvcResult login = iniciarSesion("sistema@uso916.test");
		String dentro = tokenDe(mvc.perform(post("/api/auth/iglesia-actuando")
			.header(HttpHeaders.AUTHORIZATION, bearer(tokenDe(login)))
			.cookie(new Cookie(COOKIE_REFRESCO, cookieDe(login)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"iglesiaId\":916}")).andExpect(status().isOk()).andReturn());

		mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(dentro)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.global", is(false)))
			.andExpect(jsonPath("$.usuarios", is(1)))
			.andExpect(jsonPath("$.iglesiasActivas", nullValue()));
	}

	@Test
	void unPeriodoDesconocidoResponde400() throws Exception {
		semilla.crearUsuario("admin@uso917.test", 917L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		String acceso = tokenDe(iniciarSesion("admin@uso917.test"));

		mvc.perform(get(RUTA).param("periodo", "SIGLO").header(HttpHeaders.AUTHORIZATION, bearer(acceso)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.traceId", notNullValue()));
	}

	private void actividad(long usuarioId, LocalDate fecha, Long iglesiaId, String rolBase) {
		jdbc.update("insert into actividad_usuario (usuario_id, fecha, iglesia_id, rol_base) values (?, ?, ?, ?) "
				+ "on conflict do nothing", usuarioId, fecha, iglesiaId, rolBase);
	}

	private MvcResult iniciarSesion(String correo) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, SemillaSeguridad.CONTRASENA)))
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

}
