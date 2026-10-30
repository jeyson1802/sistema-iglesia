package com.iglesia.seguridad.rol.controller;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.jayway.jsonpath.JsonPath;

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
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.seguridad.SemillaSeguridad;
import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.jwt.constant.ClaimsJwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API de roles y permisos contra PostgreSQL real, incluido el aislamiento entre iglesias. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RolIntegrationTest {

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
	void listaElCatalogoDePermisosOrdenadoPorModulo() throws Exception {
		mvc.perform(get("/api/permisos").header(HttpHeaders.AUTHORIZATION, bearer(administradorDe(51L))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(26))
			.andExpect(jsonPath("$[0].codigo").value(Permisos.APORTES_VER))
			.andExpect(jsonPath("$[0].modulo").value("Aportes"))
			.andExpect(jsonPath("$[0].descripcion").isNotEmpty());
	}

	@Test
	void losPermisosDeIglesiasSoloLosTieneElRolBaseDelSistema() {
		String permisosDe = """
				select array_to_string(array_agg(rp.permiso_codigo order by rp.permiso_codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where r.codigo = ? and rp.permiso_codigo like 'iglesias.%'""";

		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_SISTEMA"))
			.isEqualTo(Permisos.IGLESIAS_ADMINISTRAR + "," + Permisos.IGLESIAS_VER);
		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_IGLESIA")).isNull();
	}

	@Test
	void losPermisosDeNivelesYGruposLosTieneElAdministradorDeIglesiaYElLiderVeMiembros() {
		String permisosDe = """
				select array_to_string(array_agg(rp.permiso_codigo order by rp.permiso_codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where r.codigo = ? and (rp.permiso_codigo like 'niveles.%' or rp.permiso_codigo like 'grupos.%'
				                        or rp.permiso_codigo like 'miembros.%')""";

		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_IGLESIA")).isEqualTo(String.join(",",
				Permisos.GRUPOS_ADMINISTRAR, Permisos.GRUPOS_VER, "miembros.crear", "miembros.desactivar",
				"miembros.editar", Permisos.MIEMBROS_VER, Permisos.NIVELES_ADMINISTRAR));
		assertThat(jdbc.queryForObject(permisosDe, String.class, "LIDER")).isEqualTo(Permisos.MIEMBROS_VER);
		assertThat(jdbc.queryForObject(permisosDe, String.class, "MIEMBRO")).isNull();
	}

	@Test
	void losPermisosDeAportesLosTieneElAdministradorDeIglesiaYElLiderSoloVe() {
		String permisosDe = """
				select array_to_string(array_agg(rp.permiso_codigo order by rp.permiso_codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where r.codigo = ? and rp.permiso_codigo like 'aportes.%'""";

		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_IGLESIA")).isEqualTo(String.join(",",
				Permisos.APORTES_ANULAR, Permisos.APORTES_CONFIGURAR, Permisos.APORTES_EDITAR,
				Permisos.APORTES_REGISTRAR, Permisos.APORTES_VER));
		assertThat(jdbc.queryForObject(permisosDe, String.class, "LIDER")).isEqualTo(Permisos.APORTES_VER);
		assertThat(jdbc.queryForObject(permisosDe, String.class, "MIEMBRO")).isNull();
		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_SISTEMA")).isNull();
	}

	@Test
	void losPermisosDeAsistenciaLosTieneElAdministradorDeIglesiaYElLiderSoloSusGrupos() {
		String permisosDe = """
				select array_to_string(array_agg(rp.permiso_codigo order by rp.permiso_codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where r.codigo = ? and rp.permiso_codigo like 'asistencia.%'""";

		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_IGLESIA")).isEqualTo(String.join(",",
				Permisos.ASISTENCIA_CONFIGURAR, Permisos.ASISTENCIA_REGISTRAR, Permisos.ASISTENCIA_VER));
		assertThat(jdbc.queryForObject(permisosDe, String.class, "LIDER")).isEqualTo(Permisos.ASISTENCIA_GRUPO);
		assertThat(jdbc.queryForObject(permisosDe, String.class, "MIEMBRO")).isNull();
		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_SISTEMA")).isNull();
	}

	@Test
	void losPermisosDeConsolidacionLosTieneElAdministradorDeIglesiaYElLiderSoloContactar() {
		String permisosDe = """
				select array_to_string(array_agg(rp.permiso_codigo order by rp.permiso_codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where r.codigo = ? and rp.permiso_codigo like 'consolidacion.%'""";

		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_IGLESIA")).isEqualTo(String.join(",",
				Permisos.CONSOLIDACION_ADMINISTRAR, Permisos.CONSOLIDACION_CONTACTAR, Permisos.CONSOLIDACION_VER));
		assertThat(jdbc.queryForObject(permisosDe, String.class, "LIDER")).isEqualTo(Permisos.CONSOLIDACION_CONTACTAR);
		assertThat(jdbc.queryForObject(permisosDe, String.class, "MIEMBRO")).isNull();
		assertThat(jdbc.queryForObject(permisosDe, String.class, "ADMIN_SISTEMA")).isNull();
	}

	@Test
	void soloElAdministradorDeIglesiaPersonalizaSuIglesia() {
		String rolesCon = """
				select array_to_string(array_agg(r.codigo order by r.codigo), ',')
				from rol_permiso rp join rol r on r.id = rp.rol_id
				where rp.permiso_codigo = ?""";

		assertThat(jdbc.queryForObject(rolesCon, String.class, Permisos.IGLESIA_PERSONALIZAR)).isEqualTo("ADMIN_IGLESIA");
	}

	@Test
	void unRolDeIglesiaNoPuedeMarcarLosPermisosDeIglesias() throws Exception {
		String token = administradorDe(64L);

		crear(token, "Gestor de iglesias", "ADMIN_IGLESIA", Permisos.IGLESIAS_VER).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString(Permisos.IGLESIAS_VER)));
	}

	@Test
	void creaUnRolYLoRegistraSinDatosPersonales(CapturedOutput salida) throws Exception {
		String token = administradorDe(52L);

		String creado = crear(token, "Secretaría", "ADMIN_IGLESIA", "miembros.ver", "miembros.crear")
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/roles/")))
			.andExpect(jsonPath("$.nombre").value("Secretaría"))
			.andExpect(jsonPath("$.rolBase").value("ADMIN_IGLESIA"))
			.andExpect(jsonPath("$.base").value(false))
			.andExpect(jsonPath("$.activo").value(true))
			.andExpect(jsonPath("$.permisos", contains("miembros.crear", "miembros.ver")))
			.andExpect(jsonPath("$.cantidadUsuarios").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(creado, "$.id")).longValue();

		assertThat(salida.getOut()).contains("Rol creado id=" + id + " iglesiaId=52 por usuarioId=")
			.doesNotContain("admin@iglesia52.test");
	}

	@Test
	void validaElRolBaseYLosPermisos() throws Exception {
		String token = administradorDe(53L);
		long personalizado = semilla.crearRolPersonalizado(53L, "Consejo", "ADMIN_IGLESIA", true, "miembros.ver");

		crear(token, "Sin permiso", "LIDER", "usuarios.ver").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(containsString("usuarios.ver")));
		crear(token, "Sistema", "ADMIN_SISTEMA").andExpect(status().isBadRequest());
		crearConRolBaseId(token, "Heredado", personalizado).andExpect(status().isBadRequest());
		crearConRolBaseId(token, "Inexistente", 999_999L).andExpect(status().isBadRequest());
		mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\" \",\"rolBaseId\":2,\"permisos\":[]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.nombre").exists());
	}

	@Test
	void elNombreEsUnicoEnLaIglesiaSinDistinguirMayusculasPeroSePuedeRepetirEnOtra() throws Exception {
		semilla.crearRolPersonalizado(54L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		long otro = semilla.crearRolPersonalizado(54L, "Tesorería", "ADMIN_IGLESIA", true, "miembros.ver");
		String token = administradorDe(54L);

		crear(token, "SECRETARÍA", "ADMIN_IGLESIA", "miembros.ver").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ya existe un rol con ese nombre en la iglesia"));
		actualizar(token, otro, "secretaría", "miembros.ver").andExpect(status().isConflict());
		crear(administradorDe(55L), "Secretaría", "ADMIN_IGLESIA", "miembros.ver").andExpect(status().isCreated());
	}

	@Test
	void editaUnRolPersonalizadoYLoRegistra(CapturedOutput salida) throws Exception {
		long id = semilla.crearRolPersonalizado(56L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		String token = administradorDe(56L);

		actualizar(token, id, "Secretaría general", "miembros.ver", "miembros.editar").andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Secretaría general"))
			.andExpect(jsonPath("$.permisos", contains("miembros.editar", "miembros.ver")));
		actualizar(token, id, "Secretaría general", "roles.inventado").andExpect(status().isBadRequest());

		assertThat(salida.getOut()).contains("Rol actualizado id=" + id + " iglesiaId=56 por usuarioId=");
	}

	@Test
	void losRolesBaseSonDeSoloLectura() throws Exception {
		String token = administradorDe(57L);
		long lider = semilla.idRolBase("LIDER");

		actualizar(token, lider, "Líder", "miembros.ver").andExpect(status().isForbidden());
		cambiarEstado(token, lider, false).andExpect(status().isForbidden());
	}

	@Test
	void unRolDeOtraIglesiaNoExisteParaQuienConsulta() throws Exception {
		long ajeno = semilla.crearRolPersonalizado(98L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		String token = administradorDe(58L);

		mvc.perform(get("/api/roles/{id}", ajeno).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
		actualizar(token, ajeno, "Mía", "miembros.ver").andExpect(status().isNotFound());
		cambiarEstado(token, ajeno, false).andExpect(status().isNotFound());
		mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", not(hasItem((int) ajeno))))
			.andExpect(jsonPath("$[0].base").value(true))
			.andExpect(jsonPath("$[0].rolBase").value("ADMIN_SISTEMA"));
	}

	@Test
	void laCantidadDeUsuariosDeUnRolBaseSoloCuentaLosDeLaIglesiaDeQuienConsulta() throws Exception {
		long miembro = semilla.idRolBase("MIEMBRO");
		semilla.crearUsuario("miembro@iglesia59.test", 59L, 1L, miembro);
		semilla.crearUsuario("miembro@iglesia97.test", 97L, 1L, miembro);
		String token = administradorDe(59L);

		mvc.perform(get("/api/roles/{id}", miembro).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.base").value(true))
			.andExpect(jsonPath("$.cantidadUsuarios").value(1));
	}

	@Test
	void noSeDesactivaElUnicoRolActivoDeUnUsuario() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(60L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		semilla.crearUsuario("secretaria@iglesia60.test", 60L, null, secretaria);
		String token = administradorDe(60L);

		cambiarEstado(token, secretaria, false).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("sin roles activos")));
		mvc.perform(get("/api/roles/{id}", secretaria).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(jsonPath("$.activo").value(true))
			.andExpect(jsonPath("$.cantidadUsuarios").value(1));
	}

	@Test
	void desactivarQuitaLosPermisosEnElSiguienteLoginYReactivarLosDevuelve(CapturedOutput salida) throws Exception {
		long secretaria = semilla.crearRolPersonalizado(61L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		semilla.crearUsuario("doble@iglesia61.test", 61L, 3L, secretaria, semilla.idRolBase("MIEMBRO"));
		String token = administradorDe(61L);

		cambiarEstado(token, secretaria, false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

		String acceso = iniciarSesion("doble@iglesia61.test");
		assertThat(decodificador.decode(acceso).getClaimAsStringList(ClaimsJwt.PERMISOS)).isEmpty();
		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(acceso)))
			.andExpect(jsonPath("$.perfiles[*].nombre", contains("Miembro")));

		cambiarEstado(token, secretaria, true).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
		assertThat(decodificador.decode(iniciarSesion("doble@iglesia61.test")).getClaimAsStringList(ClaimsJwt.PERMISOS))
			.containsExactly("miembros.ver");

		assertThat(salida.getOut()).contains("Rol desactivado id=" + secretaria + " iglesiaId=61 por usuarioId=")
			.contains("Rol reactivado id=" + secretaria + " iglesiaId=61 por usuarioId=");
	}

	@Test
	void unaSecretariaSinRolesAdministrarRecibeAccesoDenegado() throws Exception {
		long secretaria = semilla.crearRolPersonalizado(62L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver",
				"miembros.crear");
		semilla.crearUsuario("secretaria@iglesia62.test", 62L, null, secretaria);
		String token = iniciarSesion("secretaria@iglesia62.test");

		crear(token, "Otro", "ADMIN_IGLESIA", "miembros.ver").andExpect(status().isForbidden());
		mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
	}

	@Test
	void elAdministradorDelSistemaSoloVeLosRolesBase() throws Exception {
		semilla.crearRolPersonalizado(63L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		semilla.crearUsuario("sistema@roles.test", null, null, semilla.idRolBase("ADMIN_SISTEMA"));
		String token = iniciarSesion("sistema@roles.test");

		mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[*].base", contains(true, true, true, true)));
		crear(token, "Secretaría", "ADMIN_IGLESIA", "miembros.ver").andExpect(status().isForbidden());
	}

	@Test
	void quienAdministraUsuariosSinRolesVerPuedeListarLosRolesPeroNoModificarlos() throws Exception {
		long asignador = semilla.crearRolPersonalizado(81L, "Asignador", "ADMIN_IGLESIA", true, "usuarios.ver",
				"usuarios.administrar");
		semilla.crearUsuario("asignador@iglesia81.test", 81L, null, asignador);
		String token = iniciarSesion("asignador@iglesia81.test");

		mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", hasItem("Asignador")));
		mvc.perform(get("/api/roles/{id}", asignador).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
		crear(token, "Otro", "ADMIN_IGLESIA", "miembros.ver").andExpect(status().isForbidden());
	}

	@Test
	void requiereMiembroSoloEnLiderMiembroYSusDerivados() throws Exception {
		long coordinador = semilla.crearRolPersonalizado(82L, "Coordinador", "LIDER", true);
		semilla.crearRolPersonalizado(82L, "Servidor", "MIEMBRO", true);
		semilla.crearRolPersonalizado(82L, "Secretaría", "ADMIN_IGLESIA", true, "miembros.ver");
		String token = administradorDe(82L);

		mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.rolBase == 'ADMIN_SISTEMA')].requiereMiembro", contains(false)))
			.andExpect(jsonPath("$[?(@.rolBase == 'ADMIN_IGLESIA')].requiereMiembro", contains(false, false)))
			.andExpect(jsonPath("$[?(@.rolBase == 'LIDER')].requiereMiembro", contains(true, true)))
			.andExpect(jsonPath("$[?(@.rolBase == 'MIEMBRO')].requiereMiembro", contains(true, true)));
		mvc.perform(get("/api/roles/{id}", coordinador).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.requiereMiembro").value(true));
	}

	private ResultActions crear(String token, String nombre, String rolBase, String... permisos) throws Exception {
		return crearConRolBaseId(token, nombre, semilla.idRolBase(rolBase), permisos);
	}

	private ResultActions crearConRolBaseId(String token, String nombre, long rolBaseId, String... permisos)
			throws Exception {
		return mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"rolBaseId\":%d,\"permisos\":%s}".formatted(nombre, rolBaseId,
					json(permisos))));
	}

	private ResultActions actualizar(String token, long id, String nombre, String... permisos) throws Exception {
		return mvc.perform(put("/api/roles/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"permisos\":%s}".formatted(nombre, json(permisos))));
	}

	private ResultActions cambiarEstado(String token, long id, boolean activo) throws Exception {
		return mvc.perform(patch("/api/roles/{id}/estado", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":%s}".formatted(activo)));
	}

	private String administradorDe(long iglesiaId) throws Exception {
		String correo = "admin@iglesia" + iglesiaId + ".test";
		semilla.crearUsuario(correo, iglesiaId, null, semilla.idRolBase("ADMIN_IGLESIA"));
		return iniciarSesion(correo);
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

	private static String json(String... valores) {
		return Arrays.stream(valores).map(valor -> "\"" + valor + "\"").collect(Collectors.joining(",", "[", "]"));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
