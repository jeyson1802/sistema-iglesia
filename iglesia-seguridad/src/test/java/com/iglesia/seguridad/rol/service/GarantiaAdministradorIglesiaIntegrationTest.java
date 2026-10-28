package com.iglesia.seguridad.rol.service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La iglesia nunca se queda sin un usuario activo que pueda administrar usuarios, contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GarantiaAdministradorIglesiaIntegrationTest {

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
	void noSeDesactivaNiSeQuitaElRolAlUnicoAdministradorHastaQueHayaOtro() throws Exception {
		long adminIglesia = semilla.idRolBase("ADMIN_IGLESIA");
		long unico = semilla.crearUsuario("unico@iglesia95.test", 95L, null, adminIglesia);
		semilla.crearUsuario("sistema@garantia.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		String token = iniciarSesion("sistema@garantia.test");

		esperarSinAdministrador(actualizarUsuario(token, unico, "unico@iglesia95.test", adminIglesia, null, false));
		esperarSinAdministrador(
				actualizarUsuario(token, unico, "unico@iglesia95.test", semilla.idRolBase("MIEMBRO"), 5L, true));
		assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, unico)).isTrue();
		assertThat(jdbc.queryForList("SELECT rol_id FROM usuario_rol WHERE usuario_id = ?", Long.class, unico))
			.containsExactly(adminIglesia);

		semilla.crearUsuario("otro@iglesia95.test", 95L, null, adminIglesia);
		actualizarUsuario(token, unico, "unico@iglesia95.test", adminIglesia, null, false)
			.andExpect(status().isOk());
	}

	@Test
	void noSeQuitaElPermisoNiSeDesactivaElRolDelUnicoAdministradorHastaQueHayaOtro() throws Exception {
		long coordinacion = semilla.crearRolPersonalizado(96L, "Coordinación", "ADMIN_IGLESIA", true,
				"usuarios.ver", "usuarios.administrar");
		long gestion = semilla.crearRolPersonalizado(96L, "Gestión de roles", "ADMIN_IGLESIA", true, "roles.ver",
				"roles.administrar");
		// Con MIEMBRO conserva un rol activo: así el 409 es el de la garantía y no el de «sin roles activos».
		semilla.crearUsuario("coordinador@iglesia96.test", 96L, 5L, coordinacion, semilla.idRolBase("MIEMBRO"));
		semilla.crearUsuario("gestor@iglesia96.test", 96L, null, gestion);
		String token = iniciarSesion("gestor@iglesia96.test");

		esperarSinAdministrador(actualizarRol(token, coordinacion, "usuarios.ver"));
		esperarSinAdministrador(desactivarRol(token, coordinacion));
		assertThat(jdbc.queryForList("SELECT permiso_codigo FROM rol_permiso WHERE rol_id = ?", String.class,
				coordinacion))
			.containsExactlyInAnyOrder("usuarios.ver", "usuarios.administrar");
		assertThat(jdbc.queryForObject("SELECT activo FROM rol WHERE id = ?", Boolean.class, coordinacion)).isTrue();

		semilla.crearUsuario("admin@iglesia96.test", 96L, null, semilla.idRolBase("ADMIN_IGLESIA"));
		actualizarRol(token, coordinacion, "usuarios.ver").andExpect(status().isOk());
		desactivarRol(token, coordinacion).andExpect(status().isOk());
	}

	@Test
	void dosAdministradoresQueSeDesactivanEntreSiALaVezNoDejanLaIglesiaSinNinguno() throws Exception {
		long adminIglesia = semilla.idRolBase("ADMIN_IGLESIA");
		long uno = semilla.crearUsuario("uno@iglesia100.test", 100L, null, adminIglesia);
		long dos = semilla.crearUsuario("dos@iglesia100.test", 100L, null, adminIglesia);
		String tokenUno = iniciarSesion("uno@iglesia100.test");
		String tokenDos = iniciarSesion("dos@iglesia100.test");
		CountDownLatch salida = new CountDownLatch(1);

		List<Integer> estados;
		try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
			Future<Integer> unoADos = hilos.submit(desactivarAlSalir(salida, tokenUno, dos, "dos@iglesia100.test"));
			Future<Integer> dosAUno = hilos.submit(desactivarAlSalir(salida, tokenDos, uno, "uno@iglesia100.test"));
			salida.countDown();
			estados = List.of(unoADos.get(), dosAUno.get());
		}

		assertThat(estados).containsExactlyInAnyOrder(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE iglesia_id = 100 AND activo", Long.class))
			.isEqualTo(1L);
	}

	private Callable<Integer> desactivarAlSalir(CountDownLatch salida, String token, long usuarioId,
			String correo) {
		long adminIglesia = semilla.idRolBase("ADMIN_IGLESIA");
		return () -> {
			salida.await();
			return actualizarUsuario(token, usuarioId, correo, adminIglesia, null, false).andReturn()
				.getResponse()
				.getStatus();
		};
	}

	private static void esperarSinAdministrador(ResultActions resultado) throws Exception {
		resultado.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(GarantiaAdministradorIglesia.SIN_ADMINISTRADOR));
	}

	private ResultActions actualizarUsuario(String token, long id, String correo, long rolId, Long miembroId,
			boolean activo) throws Exception {
		return mvc.perform(put("/api/usuarios/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"correo\":\"%s\",\"rolIds\":[%d],\"miembroId\":%s,\"activo\":%s}".formatted(correo, rolId,
					miembroId, activo)));
	}

	private ResultActions actualizarRol(String token, long id, String permiso) throws Exception {
		return mvc.perform(put("/api/roles/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Coordinación\",\"permisos\":[\"%s\"]}".formatted(permiso)));
	}

	private ResultActions desactivarRol(String token, long id) throws Exception {
		return mvc.perform(patch("/api/roles/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}"));
	}

	private String iniciarSesion(String correo) throws Exception {
		String respuesta = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, SemillaSeguridad.CONTRASENA)))
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
