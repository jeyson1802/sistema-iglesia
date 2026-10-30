package com.iglesia.seguridad.jwt.service;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Claims del login contra PostgreSQL real con roles personalizados sembrados por SQL. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GeneradorTokenAccesoIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JwtDecoder decodificador;

	@Autowired
	private JwtAuthenticationConverter convertidor;

	private SemillaSeguridad semilla;

	@BeforeEach
	void configurar() {
		semilla = new SemillaSeguridad(jdbc, codificador);
	}

	@Test
	void unaSecretariaRecibeSuRolBaseYSoloSusPermisos() throws Exception {
		long iglesiaId = 31L;
		long secretaria = semilla.crearRolPersonalizado(iglesiaId, "Secretaría", "ADMIN_IGLESIA", true,
				"miembros.ver", "miembros.crear");
		long tesoreria = semilla.crearRolPersonalizado(iglesiaId, "Tesorería", "ADMIN_IGLESIA", false,
				"usuarios.ver");
		semilla.crearUsuario("secretaria@iglesia31.test", iglesiaId, null, secretaria, tesoreria);

		Jwt jwt = iniciarSesion("secretaria@iglesia31.test");

		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_IGLESIA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("miembros.crear", "miembros.ver");
		// Spring Security agrega además su propia autoridad de factor (FACTOR_BEARER).
		assertThat(convertidor.convert(jwt).getAuthorities()).map(GrantedAuthority::getAuthority)
			.contains("ROLE_ADMIN_IGLESIA", "miembros.crear", "miembros.ver")
			.doesNotContain("usuarios.ver");
	}

	@Test
	void elAdministradorDeIglesiaRecibeTodosLosPermisosDeSuIglesia() throws Exception {
		semilla.crearUsuario("admin@iglesia32.test", 32L, null, semilla.idRolBase("ADMIN_IGLESIA"));

		Jwt jwt = iniciarSesion("admin@iglesia32.test");

		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_IGLESIA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("aportes.anular",
				"aportes.configurar", "aportes.editar", "aportes.registrar", "aportes.ver", "asistencia.configurar",
				"asistencia.registrar", "asistencia.ver", "consolidacion.administrar", "consolidacion.contactar",
				"consolidacion.ver", "grupos.administrar", "grupos.ver", "iglesia.personalizar", "miembros.crear",
				"miembros.desactivar", "miembros.editar", "miembros.ver", "niveles.administrar", "roles.administrar",
				"roles.ver", "usuarios.administrar", "usuarios.ver");
	}

	private Jwt iniciarSesion(String correo) throws Exception {
		String respuesta = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, SemillaSeguridad.CONTRASENA)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return decodificador.decode(JsonPath.read(respuesta, "$.tokenAcceso"));
	}

}
