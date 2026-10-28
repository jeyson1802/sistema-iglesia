package com.iglesia.seguridad.common.seguridad.filter;

import com.jayway.jsonpath.JsonPath;

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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.common.error.component.FabricaProblemDetail;
import com.iglesia.seguridad.common.seguridad.exception.CambioContrasenaPendienteException;
import com.iglesia.seguridad.common.traza.constant.TrazaConstantes;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un usuario con contraseña temporal solo puede consultar su usuario y cambiarla, contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CambioContrasenaPendienteIntegrationTest {

	private static final String CONTRASENA_NUEVA = "ClaveDefinitiva123";

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
	void conContrasenaTemporalSoloPermiteConsultarSuUsuarioYCambiarla() throws Exception {
		String token = administradorConContrasenaTemporal("temporal@iglesia71.test", 71L);

		assertThat(cambioContrasena(token)).isTrue();
		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk());
		esperarCambioPendiente(mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))));
		esperarCambioPendiente(mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token))));
	}

	@Test
	void despuesDeCambiarLaContrasenaElNuevoLoginYaNoTieneElCambioPendiente() throws Exception {
		String correo = "temporal@iglesia72.test";
		String token = administradorConContrasenaTemporal(correo, 72L);

		mvc.perform(put("/api/usuarios/me/contrasena").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"contrasenaActual\":\"%s\",\"contrasenaNueva\":\"%s\"}".formatted(SemillaSeguridad.CONTRASENA,
					CONTRASENA_NUEVA)))
			.andExpect(status().isNoContent());

		String nuevoToken = iniciarSesion(correo, CONTRASENA_NUEVA);
		assertThat(cambioContrasena(nuevoToken)).isFalse();
		mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(nuevoToken)))
			.andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT debe_cambiar_contrasena FROM usuario WHERE correo = ?",
				Boolean.class, correo))
			.isFalse();
	}

	@Test
	void sinContrasenaTemporalElLoginEmiteElClaimEnFalse() throws Exception {
		semilla.crearUsuario("definitiva@iglesia73.test", 73L, null, semilla.idRolBase("ADMIN_IGLESIA"));

		assertThat(cambioContrasena(iniciarSesion("definitiva@iglesia73.test", SemillaSeguridad.CONTRASENA)))
			.isFalse();
	}

	private void esperarCambioPendiente(ResultActions resultado) throws Exception {
		resultado.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value(FabricaProblemDetail.TITULO_ACCESO_DENEGADO))
			.andExpect(jsonPath("$.detail").value(CambioContrasenaPendienteException.DETALLE))
			.andExpect(jsonPath("$." + TrazaConstantes.PROPIEDAD_TRACE_ID).isNotEmpty())
			.andExpect(header().exists(TrazaConstantes.CABECERA_TRACE_ID));
	}

	private String administradorConContrasenaTemporal(String correo, long iglesiaId) throws Exception {
		long id = semilla.crearUsuario(correo, iglesiaId, null, semilla.idRolBase("ADMIN_IGLESIA"));
		semilla.exigirCambioContrasena(id);
		return iniciarSesion(correo, SemillaSeguridad.CONTRASENA);
	}

	private Boolean cambioContrasena(String token) {
		return decodificador.decode(token).getClaim(ClaimsJwt.CAMBIO_CONTRASENA);
	}

	private String iniciarSesion(String correo, String contrasena) throws Exception {
		String respuesta = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(respuesta, "$.tokenAcceso");
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
