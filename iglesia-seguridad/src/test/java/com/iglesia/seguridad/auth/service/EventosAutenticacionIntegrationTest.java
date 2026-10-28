package com.iglesia.seguridad.auth.service;

import com.jayway.jsonpath.JsonPath;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los intentos de inicio de sesión quedan en el log con el id del usuario y la IP, nunca con el correo ni la
 * contraseña.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "seguridad.admin-inicial.correo=" + EventosAutenticacionIntegrationTest.CORREO_ADMIN,
		"seguridad.admin-inicial.contrasena=" + EventosAutenticacionIntegrationTest.CONTRASENA_ADMIN })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class EventosAutenticacionIntegrationTest {

	// Los cuerpos usan rolIds [2]: el id del rol base ADMIN_IGLESIA sembrado por Flyway (V2).

	static final String CORREO_ADMIN = "admin@sistema.test";

	static final String CONTRASENA_ADMIN = "AdminPrueba123";

	private static final String IP_CLIENTE = "203.0.113.7";

	private static final long IGLESIA = 101L;

	private static final String CONTRASENA_ERRONEA = "ClaveErronea987";

	@Autowired
	private MockMvc mvc;

	@Test
	void contrasenaIncorrectaRegistraUsuarioIdEIpSinCorreoNiContrasena(CapturedOutput salida) throws Exception {
		iniciarSesion(CORREO_ADMIN, CONTRASENA_ERRONEA).andExpect(status().isUnauthorized());

		assertThat(salida.getOut()).containsPattern("fallido usuarioId=\\d+ ip=" + IP_CLIENTE)
			.doesNotContain(CORREO_ADMIN)
			.doesNotContain(CONTRASENA_ERRONEA);
	}

	@Test
	void correoInexistenteRegistraUsuarioInexistenteSinElCorreo(CapturedOutput salida) throws Exception {
		String correo = "nadie@inexistente.test";

		iniciarSesion(correo, CONTRASENA_ERRONEA).andExpect(status().isUnauthorized());

		assertThat(salida.getOut())
			.contains("fallido usuarioId=" + AuthService.USUARIO_INEXISTENTE + " ip=" + IP_CLIENTE)
			.doesNotContain(correo)
			.doesNotContain(CONTRASENA_ERRONEA);
	}

	@Test
	void usuarioInactivoRegistraElRechazoConSuId(CapturedOutput salida) throws Exception {
		String correo = "inactivo@eventos.test";
		String contrasena = "ClaveInactivo123";
		String acceso = JsonPath.read(iniciarSesion(CORREO_ADMIN, CONTRASENA_ADMIN).andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.tokenAcceso");
		// Otro administrador activo en la iglesia: sin él, desactivar al único respondería 409.
		crearAdministrador(acceso, "admin@eventos.test", contrasena);
		Integer id = crearAdministrador(acceso, correo, contrasena);
		mvc.perform(put("/api/usuarios/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"rolIds\":[2],\"activo\":false}".formatted(correo)))
			.andExpect(status().isOk());

		iniciarSesion(correo, contrasena).andExpect(status().isUnauthorized());

		assertThat(salida.getOut()).contains("Usuario creado id=" + id + " iglesiaId=" + IGLESIA + " por usuarioId=")
			.contains("Usuario actualizado id=" + id + " por usuarioId=")
			.contains("usuario inactivo usuarioId=" + id + " ip=" + IP_CLIENTE)
			.doesNotContain(correo)
			.doesNotContain(contrasena);
	}

	@Test
	void loginExitosoRegistraSoloElUsuarioId(CapturedOutput salida) throws Exception {
		iniciarSesion(CORREO_ADMIN, CONTRASENA_ADMIN).andExpect(status().isOk());

		assertThat(salida.getOut()).containsPattern("exitoso usuarioId=\\d+")
			.doesNotContain(CORREO_ADMIN)
			.doesNotContain(CONTRASENA_ADMIN);
	}

	private Integer crearAdministrador(String acceso, String correo, String contrasena) throws Exception {
		return JsonPath.read(mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"correo":"%s","contrasena":"%s","iglesiaId":%d,"rolIds":[2]}
					""".formatted(correo, contrasena, IGLESIA)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.id");
	}

	private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
		return mvc.perform(post("/api/auth/login").with(peticion -> {
			peticion.setRemoteAddr(IP_CLIENTE);
			return peticion;
		})
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)));
	}

}
