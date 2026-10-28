package com.iglesia.seguridad.usuario.controller;

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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API de usuarios con roles por id y autorización por permiso, contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class UsuarioIntegrationTest {

	private static final String CONTRASENA_TEMPORAL = "TemporalRestablecida123";

	private static final String COOKIE_REFRESCO = "iglesia_refresco";

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
	void unaSecretariaSinPermisosDeUsuariosRecibeAccesoDenegado() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(41L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver",
				"miembros.crear");
		semilla.crearUsuario("secretaria@iglesia41.test", 41L, null, secretaria);
		String token = iniciarSesion("secretaria@iglesia41.test");

		mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(crearUsuario("nuevo@iglesia41.test", 41L, null, semilla.idRolBase("ADMIN_IGLESIA"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void elAdministradorDeIglesiaAsignaUnRolPersonalizadoDeSuIglesia() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(42L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		String token = administradorDe(42L);

		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(crearUsuario("secretaria@iglesia42.test", 42L, null, secretaria)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.debeCambiarContrasena").value(true))
			.andExpect(jsonPath("$.roles[0].id").value(secretaria))
			.andExpect(jsonPath("$.roles[0].nombre").value("Secretaría"))
			.andExpect(jsonPath("$.roles[0].rolBase").value("ADMIN_IGLESIA"));
	}

	@Test
	void actualizarConUnRolDeOtraIglesiaOInactivoEsUnErrorDeDatos() throws Exception {
		long ajeno = semilla.crearRolPersonalizado(99L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		long inactivo = semilla.crearRolPersonalizado(43L, "Tesorería", "ADMIN_IGLESIA", false, "usuarios.ver");
		long usuario = semilla.crearUsuario("miembro@iglesia43.test", 43L, 5L, semilla.idRolBase("MIEMBRO"));
		String token = administradorDe(43L);

		actualizar(token, usuario, "miembro@iglesia43.test", ajeno).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("no existe")));
		actualizar(token, usuario, "miembro@iglesia43.test", inactivo).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("inactivo")));
	}

	@Test
	void elAdministradorDeIglesiaNoOtorgaAdministradorDelSistema() throws Exception {
		long usuario = semilla.crearUsuario("miembro@iglesia44.test", 44L, 5L, semilla.idRolBase("MIEMBRO"));
		String token = administradorDe(44L);

		actualizar(token, usuario, "miembro@iglesia44.test", semilla.idRolBase("ADMIN_SISTEMA"))
			.andExpect(status().isForbidden());
	}

	@Test
	void unRolQueHeredaDeLiderExigeMiembroVinculado() throws Exception {
		long coordinador = semilla.crearRolPersonalizado(45L, "Coordinador", "LIDER", true);
		String token = administradorDe(45L);

		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(crearUsuario("coordinador@iglesia45.test", 45L, null, coordinador)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("miembro vinculado")));
	}

	@Test
	void miUsuarioDevuelveUnPerfilPorRolActivo() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(46L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver",
				"miembros.crear");
		long tesoreria = semilla.crearRolPersonalizado(46L, "Tesorería", "ADMIN_IGLESIA", false, "usuarios.ver");
		semilla.crearUsuario("perfiles@iglesia46.test", 46L, 8L, semilla.idRolBase("MIEMBRO"), secretaria,
				tesoreria);
		String token = iniciarSesion("perfiles@iglesia46.test");

		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.miembroId").value(8))
			.andExpect(jsonPath("$.debeCambiarContrasena").value(false))
			.andExpect(jsonPath("$.perfiles.length()").value(2))
			.andExpect(jsonPath("$.perfiles[0].id").value(secretaria))
			.andExpect(jsonPath("$.perfiles[0].nombre").value("Secretaría"))
			.andExpect(jsonPath("$.perfiles[0].rolBase").value("ADMIN_IGLESIA"))
			.andExpect(jsonPath("$.perfiles[0].permisos", contains("miembros.crear", "miembros.ver")))
			.andExpect(jsonPath("$.perfiles[1].nombre").value("Miembro"))
			.andExpect(jsonPath("$.perfiles[1].rolBase").value("MIEMBRO"))
			.andExpect(jsonPath("$.roles").doesNotExist());
	}

	@Test
	void filtraPorParteDelCorreoSinDistinguirMayusculasYSinComodines() throws Exception {
		semilla.crearUsuario("prueba.uno@iglesia83.test", 83L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.crearUsuario("otra.prueba@iglesia83.test", 83L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.crearUsuario("ajeno@iglesia83.test", 83L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.crearUsuario("prueba@iglesia84.test", 84L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		String token = administradorDe(83L);

		listar(token, "correo", "PRUEBA").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].correo",
					contains("otra.prueba@iglesia83.test", "prueba.uno@iglesia83.test")));
		// % y _ se buscan literalmente: ningún correo los contiene.
		listar(token, "correo", "%").andExpect(jsonPath("$.content.length()").value(0));
		listar(token, "correo", "_").andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void filtraPorEstadoYPorRolYCombinaLosFiltros() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(85L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		semilla.crearUsuario("activa@iglesia85.test", 85L, null, secretaria);
		long inactiva = semilla.crearUsuario("inactiva@iglesia85.test", 85L, null, secretaria);
		semilla.desactivarUsuario(inactiva);
		String token = administradorDe(85L);

		listar(token, "activo", "false").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].correo", contains("inactiva@iglesia85.test")));
		listar(token, "rolId", String.valueOf(secretaria))
			.andExpect(jsonPath("$.content[*].correo", contains("activa@iglesia85.test", "inactiva@iglesia85.test")));
		listar(token, "rolId", String.valueOf(secretaria), "activo", "true", "correo", "ACTIVA")
			.andExpect(jsonPath("$.content[*].correo", contains("activa@iglesia85.test")))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void unAdministradorDeIglesiaNoVeOtraIglesiaAunqueEnvieSuIglesiaId() throws Exception {
		semilla.crearUsuario("vecino@iglesia86.test", 86L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		String token = administradorDe(87L);

		listar(token, "iglesiaId", "86").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].correo", contains("admin@iglesia87.test")));
		listar(token, "iglesiaId", "86", "correo", "vecino").andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void unCorreoDeFiltroDemasiadoLargoEsUnErrorDeDatos() throws Exception {
		listar(administradorDe(88L), "correo", "a".repeat(255)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.correo").exists());
	}

	@Test
	void restablecerLaContrasenaLaVuelveTemporalYCierraLasSesiones(CapturedOutput salida) throws Exception {
		long miembro = semilla.crearUsuario("restablecer@iglesia89.test", 89L, 5L, semilla.idRolBase("MIEMBRO"));
		Cookie refrescoAnterior = login("restablecer@iglesia89.test", SemillaSeguridad.CONTRASENA).getResponse()
			.getCookie(COOKIE_REFRESCO);
		String token = administradorDe(89L);

		restablecer(token, miembro, CONTRASENA_TEMPORAL).andExpect(status().isNoContent());

		mvc.perform(get("/api/usuarios/{id}", miembro).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(jsonPath("$.debeCambiarContrasena").value(true));
		mvc.perform(post("/api/auth/refresh").cookie(refrescoAnterior)).andExpect(status().isUnauthorized());
		String tokenTemporal = JsonPath.read(
				login("restablecer@iglesia89.test", CONTRASENA_TEMPORAL).getResponse().getContentAsString(),
				"$.tokenAcceso");
		assertThat(decodificador.decode(tokenTemporal).<Boolean>getClaim(ClaimsJwt.CAMBIO_CONTRASENA)).isTrue();
		assertThat(salida.getOut()).contains("Contraseña restablecida usuarioId=" + miembro + " por usuarioId=")
			.doesNotContain(CONTRASENA_TEMPORAL);
	}

	@Test
	void noSeRestableceLaPropiaContrasenaNiUnaDemasiadoCorta() throws Exception {
		long miembro = semilla.crearUsuario("corta@iglesia90.test", 90L, 5L, semilla.idRolBase("MIEMBRO"));
		long admin = semilla.crearUsuario("admin@iglesia90.test", 90L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		String token = iniciarSesion("admin@iglesia90.test");

		restablecer(token, admin, CONTRASENA_TEMPORAL).andExpect(status().isBadRequest());
		restablecer(token, miembro, "Corta12").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.contrasenaTemporal").exists());
		assertThat(debeCambiarContrasena(miembro)).isFalse();
	}

	@Test
	void noSeRestableceLaContrasenaDeOtraIglesiaNiSinUsuariosAdministrar() throws Exception {
		long ajeno = semilla.crearUsuario("ajeno@iglesia91.test", 91L, 5L, semilla.idRolBase("MIEMBRO"));
		long consulta = semilla.crearRolPersonalizado(92L, "Consulta", "ADMIN_IGLESIA", true, "usuarios.ver");
		long companero = semilla.crearUsuario("companero@iglesia92.test", 92L, 5L, semilla.idRolBase("MIEMBRO"));
		semilla.crearUsuario("consulta@iglesia92.test", 92L, null, consulta);

		restablecer(administradorDe(92L), ajeno, CONTRASENA_TEMPORAL).andExpect(status().isNotFound());
		restablecer(iniciarSesion("consulta@iglesia92.test"), companero, CONTRASENA_TEMPORAL)
			.andExpect(status().isForbidden());
		assertThat(debeCambiarContrasena(ajeno)).isFalse();
		assertThat(debeCambiarContrasena(companero)).isFalse();
	}

	@Test
	void cambiarElCorreoLoNormalizaYCierraLasSesionesSinRegistrarCorreos(CapturedOutput salida) throws Exception {
		long miembro = semilla.crearUsuario("anterior@iglesia93.test", 93L, 5L, semilla.idRolBase("MIEMBRO"));
		Cookie refrescoAnterior = login("anterior@iglesia93.test", SemillaSeguridad.CONTRASENA).getResponse()
			.getCookie(COOKIE_REFRESCO);

		actualizar(administradorDe(93L), miembro, "Nuevo@Iglesia93.TEST", semilla.idRolBase("MIEMBRO"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.correo").value("nuevo@iglesia93.test"));

		mvc.perform(post("/api/auth/refresh").cookie(refrescoAnterior)).andExpect(status().isUnauthorized());
		login("nuevo@iglesia93.test", SemillaSeguridad.CONTRASENA);
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"anterior@iglesia93.test\",\"contrasena\":\"%s\"}"
				.formatted(SemillaSeguridad.CONTRASENA)))
			.andExpect(status().isUnauthorized());
		assertThat(salida.getOut()).contains("Correo cambiado usuarioId=" + miembro + " por usuarioId=")
			.doesNotContain("anterior@iglesia93.test")
			.doesNotContain("nuevo@iglesia93.test");
	}

	@Test
	void elCorreoDeOtroUsuarioOInvalidoNoCambiaNada() throws Exception {
		long uno = semilla.crearUsuario("uno@iglesia94.test", 94L, 5L, semilla.idRolBase("MIEMBRO"));
		semilla.crearUsuario("dos@iglesia94.test", 94L, 6L, semilla.idRolBase("MIEMBRO"));
		String token = administradorDe(94L);

		actualizar(token, uno, "DOS@iglesia94.test", semilla.idRolBase("MIEMBRO")).andExpect(status().isConflict());
		actualizar(token, uno, "no-es-un-correo", semilla.idRolBase("MIEMBRO")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.correo").exists());
		assertThat(correoDe(uno)).isEqualTo("uno@iglesia94.test");
	}

	private ResultActions restablecer(String token, long usuarioId, String contrasenaTemporal) throws Exception {
		return mvc.perform(put("/api/usuarios/{id}/contrasena", usuarioId)
			.header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"contrasenaTemporal\":\"%s\"}".formatted(contrasenaTemporal)));
	}

	private boolean debeCambiarContrasena(long usuarioId) {
		return jdbc.queryForObject("SELECT debe_cambiar_contrasena FROM usuario WHERE id = ?", Boolean.class,
				usuarioId);
	}

	/** Lista ordenado por correo con los filtros dados como pares nombre, valor. */
	private ResultActions listar(String token, String... filtros) throws Exception {
		MockHttpServletRequestBuilder peticion = get("/api/usuarios").param("sort", "correo,asc")
			.header(HttpHeaders.AUTHORIZATION, bearer(token));
		for (int i = 0; i < filtros.length; i += 2) {
			peticion.param(filtros[i], filtros[i + 1]);
		}
		return mvc.perform(peticion);
	}

	private String administradorDe(long iglesiaId) throws Exception {
		String correo = "admin@iglesia" + iglesiaId + ".test";
		semilla.crearUsuario(correo, iglesiaId, null, semilla.idRolBase("ADMIN_IGLESIA"));
		return iniciarSesion(correo);
	}

	private ResultActions actualizar(String token, long usuarioId, String correo, long rolId) throws Exception {
		return mvc.perform(put("/api/usuarios/{id}", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"rolIds\":[%d],\"miembroId\":5,\"activo\":true}".formatted(correo, rolId)));
	}

	private String correoDe(long usuarioId) {
		return jdbc.queryForObject("SELECT correo FROM usuario WHERE id = ?", String.class, usuarioId);
	}

	private String iniciarSesion(String correo) throws Exception {
		return JsonPath.read(login(correo, SemillaSeguridad.CONTRASENA).getResponse().getContentAsString(),
				"$.tokenAcceso");
	}

	private MvcResult login(String correo, String contrasena) throws Exception {
		return mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
			.andExpect(status().isOk())
			.andReturn();
	}

	private static String crearUsuario(String correo, long iglesiaId, Long miembroId, long rolId) {
		return """
				{"correo":"%s","contrasena":"ClaveSegura123","iglesiaId":%d,"miembroId":%s,"rolIds":[%d]}
				""".formatted(correo, iglesiaId, miembroId, rolId);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
