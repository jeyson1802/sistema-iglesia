package com.iglesia.seguridad.auth;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo completo contra PostgreSQL real: login, uso del token, administración y refresco con rotación. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "seguridad.admin-inicial.correo=admin@sistema.test",
		"seguridad.admin-inicial.contrasena=" + AuthIntegrationTest.CONTRASENA_ADMIN })
@AutoConfigureMockMvc
class AuthIntegrationTest {

	static final String CONTRASENA_ADMIN = "AdminPrueba123";

	@Autowired
	private MockMvc mvc;

	@Test
	void flujoCompletoDeSesion() throws Exception {
		MvcResult login = iniciarSesion("ADMIN@sistema.test", CONTRASENA_ADMIN);
		String acceso = JsonPath.read(login.getResponse().getContentAsString(), "$.tokenAcceso");
		String refresco = JsonPath.read(login.getResponse().getContentAsString(), "$.tokenRefresco");

		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles[0]").value("ADMIN_SISTEMA"));

		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"correo":"admin@iglesia1.test","contrasena":"ClaveIglesia123","iglesiaId":1,"roles":["ADMIN_IGLESIA"]}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.iglesiaId").value(1));

		MvcResult rotado = refrescar(refresco).andExpect(status().isOk()).andReturn();
		String refrescoNuevo = JsonPath.read(rotado.getResponse().getContentAsString(), "$.tokenRefresco");

		// Reutilizar el token ya rotado invalida también el nuevo (posible robo).
		refrescar(refresco).andExpect(status().isUnauthorized());
		refrescar(refrescoNuevo).andExpect(status().isUnauthorized());
	}

	@Test
	void elAdministradorDeIglesiaNoAccedeAOtraIglesia() throws Exception {
		String accesoSistema = JsonPath.read(
				iniciarSesion("admin@sistema.test", CONTRASENA_ADMIN).getResponse().getContentAsString(),
				"$.tokenAcceso");
		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + accesoSistema)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"correo":"admin@iglesia2.test","contrasena":"ClaveIglesia123","iglesiaId":2,"roles":["ADMIN_IGLESIA"]}
					"""))
			.andExpect(status().isCreated());

		String accesoIglesia = JsonPath.read(
				iniciarSesion("admin@iglesia2.test", "ClaveIglesia123").getResponse().getContentAsString(),
				"$.tokenAcceso");

		mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + accesoIglesia)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"correo":"intruso@iglesia3.test","contrasena":"ClaveIglesia123","iglesiaId":3,"roles":["ADMIN_IGLESIA"]}
					"""))
			.andExpect(status().isForbidden());

		mvc.perform(get("/api/usuarios/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + accesoIglesia))
			.andExpect(status().isNotFound());
	}

	@Test
	void credencialesInvalidasYPeticionesSinToken() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"admin@sistema.test\",\"contrasena\":\"incorrecta\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("No autenticado"));

		mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void publicaLasClavesPublicasSinPartePrivada() throws Exception {
		mvc.perform(get("/oauth2/jwks"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.keys[0].kty").value("RSA"))
			.andExpect(jsonPath("$.keys[0].alg").value("RS256"))
			.andExpect(jsonPath("$.keys[0].d").doesNotExist());
	}

	@Test
	void corsSoloPermiteLosOrigenesDelFrontend() throws Exception {
		mvc.perform(options("/api/auth/login").header(HttpHeaders.ORIGIN, "http://localhost:4200")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

		mvc.perform(options("/api/auth/login").header(HttpHeaders.ORIGIN, "http://malicioso.test")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
			.andExpect(status().isForbidden());
	}

	private MvcResult iniciarSesion(String correo, String contrasena) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
			.andExpect(status().isOk())
			.andReturn();
	}

	private ResultActions refrescar(String token) throws Exception {
		return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"tokenRefresco\":\"%s\"}".formatted(token)));
	}

}
