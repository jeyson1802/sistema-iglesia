package com.iglesia.seguridad.jwt.service;

import com.jayway.jsonpath.JsonPath;

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

import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Claims del login contra PostgreSQL real con roles personalizados. Los roles y sus asignaciones se
 * siembran por SQL porque la API de roles y la asignación por {@code rolIds} llegan en pasos posteriores.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GeneradorTokenAccesoIntegrationTest {

	private static final String CONTRASENA = "ClavePrueba123";

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

	@Test
	void unaSecretariaRecibeSuRolBaseYSoloSusPermisos() throws Exception {
		long iglesiaId = 31L;
		long secretaria = crearRolPersonalizado(iglesiaId, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver",
				"miembros.crear");
		long tesoreria = crearRolPersonalizado(iglesiaId, "Tesorería", "ADMIN_IGLESIA", false, "usuarios.ver");
		crearUsuario("secretaria@iglesia31.test", iglesiaId, secretaria, tesoreria);

		Jwt jwt = iniciarSesion("secretaria@iglesia31.test");

		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_IGLESIA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("miembros.crear", "miembros.ver");
		// Spring Security agrega además su propia autoridad de factor (FACTOR_BEARER).
		assertThat(convertidor.convert(jwt).getAuthorities()).map(GrantedAuthority::getAuthority)
			.contains("ROLE_ADMIN_IGLESIA", "miembros.crear", "miembros.ver")
			.doesNotContain("usuarios.ver");
	}

	@Test
	void elAdministradorDeIglesiaRecibeLosOchoPermisos() throws Exception {
		crearUsuario("admin@iglesia32.test", 32L, idRolBase("ADMIN_IGLESIA"));

		Jwt jwt = iniciarSesion("admin@iglesia32.test");

		assertThat(jwt.getClaimAsStringList(ClaimsJwt.ROLES)).containsExactly("ADMIN_IGLESIA");
		assertThat(jwt.getClaimAsStringList(ClaimsJwt.PERMISOS)).containsExactly("miembros.crear",
				"miembros.desactivar", "miembros.editar", "miembros.ver", "roles.administrar", "roles.ver",
				"usuarios.administrar", "usuarios.ver");
	}

	private Jwt iniciarSesion(String correo) throws Exception {
		String respuesta = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CONTRASENA)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return decodificador.decode(JsonPath.read(respuesta, "$.tokenAcceso"));
	}

	private long idRolBase(String codigo) {
		return jdbc.queryForObject("SELECT id FROM rol WHERE codigo = ?", Long.class, codigo);
	}

	private long crearRolPersonalizado(long iglesiaId, String nombre, String rolBase, boolean activo,
			String... permisos) {
		long id = jdbc.queryForObject("""
				INSERT INTO rol (iglesia_id, nombre, rol_base_id, activo, creado_en, actualizado_en)
				VALUES (?, ?, ?, ?, now(), now()) RETURNING id
				""", Long.class, iglesiaId, nombre, idRolBase(rolBase), activo);
		for (String permiso : permisos) {
			jdbc.update("INSERT INTO rol_permiso (rol_id, permiso_codigo) VALUES (?, ?)", id, permiso);
		}
		return id;
	}

	private void crearUsuario(String correo, long iglesiaId, long... rolIds) {
		long id = jdbc.queryForObject("""
				INSERT INTO usuario (correo, hash_contrasena, iglesia_id, creado_en, actualizado_en)
				VALUES (?, ?, ?, now(), now()) RETURNING id
				""", Long.class, correo, codificador.encode(CONTRASENA), iglesiaId);
		for (long rolId : rolIds) {
			jdbc.update("INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?)", id, rolId);
		}
	}

}
