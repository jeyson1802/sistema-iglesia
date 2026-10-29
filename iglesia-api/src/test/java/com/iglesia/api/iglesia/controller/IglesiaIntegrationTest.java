package com.iglesia.api.iglesia.controller;

import java.util.List;
import java.util.UUID;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catálogo de iglesias contra PostgreSQL real: alta, renombrado, unicidad del nombre y permisos. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class IglesiaIntegrationTest {

	private static final String RUTA = "/api/iglesias";

	private static final String ID_ADMIN_SISTEMA = "7";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void daDeAltaUnaIglesiaYLaRegistraSinSuNombre(CapturedOutput salida) throws Exception {
		String nombre = unico("Iglesia Central");

		String creada = crear("  " + nombre + "  ").andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, containsString(RUTA + "/")))
			.andExpect(jsonPath("$.nombre").value(nombre))
			.andExpect(jsonPath("$.creadaEn").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(creada, "$.id")).longValue();

		mvc.perform(get(RUTA + "/{id}", id).with(administradorDelSistema()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value(nombre));
		assertThat(salida.getOut()).contains("Iglesia creada id=" + id + " por usuarioId=" + ID_ADMIN_SISTEMA)
			.doesNotContain(nombre);
	}

	@Test
	void listaLasIglesiasOrdenadasPorNombre() throws Exception {
		String sufijo = UUID.randomUUID().toString();
		crear("Zion " + sufijo).andExpect(status().isCreated());
		crear("Betel " + sufijo).andExpect(status().isCreated());

		String lista = mvc.perform(get(RUTA).with(administradorDelSistema()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", hasItems("Betel " + sufijo, "Zion " + sufijo)))
			.andReturn()
			.getResponse()
			.getContentAsString();

		// Solo el orden relativo de las propias: el resto de pruebas crea nombres que la intercalación de
		// PostgreSQL puede ordenar distinto que Java.
		List<String> nombres = JsonPath.read(lista, "$[*].nombre");
		assertThat(nombres.indexOf("Betel " + sufijo)).isLessThan(nombres.indexOf("Zion " + sufijo));
	}

	@Test
	void elNombreEsUnicoSinDistinguirMayusculas() throws Exception {
		String nombre = unico("Iglesia Sion");
		crear(nombre).andExpect(status().isCreated());
		long otra = idDe(crear(unico("Iglesia Betel")).andExpect(status().isCreated()));

		crear(nombre.toUpperCase()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ya existe una iglesia con ese nombre"))
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		renombrar(otra, nombre.toLowerCase()).andExpect(status().isConflict());
	}

	@Test
	void renombraUnaIglesiaIncluidoCambiarSoloLasMayusculas(CapturedOutput salida) throws Exception {
		long id = idDe(crear(unico("Iglesia Emanuel")).andExpect(status().isCreated()));
		String nuevo = unico("Emanuel Renovada");

		renombrar(id, nuevo).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value(nuevo));
		renombrar(id, nuevo.toUpperCase()).andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value(nuevo.toUpperCase()));
		renombrar(999_999L, unico("Inexistente")).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Iglesia no encontrada"));
		assertThat(salida.getOut()).contains("Iglesia renombrada id=" + id + " por usuarioId=" + ID_ADMIN_SISTEMA)
			.doesNotContain(nuevo);
	}

	@Test
	void validaElNombre() throws Exception {
		crear(" ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.nombre").exists());
		crear("x".repeat(151)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.nombre").exists());
		mvc.perform(post(RUTA).with(administradorDelSistema()).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void exigeElPermisoDeCadaAccionYUnToken() throws Exception {
		JwtRequestPostProcessor soloVer = jwt().authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_VER));
		JwtRequestPostProcessor adminIglesia = jwt().jwt(token -> token.claim(ClaimsJwt.IGLESIA_ID, 1L))
			.authorities(new SimpleGrantedAuthority("usuarios.administrar"));

		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA).with(adminIglesia))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		mvc.perform(get(RUTA).with(soloVer)).andExpect(status().isOk());
		mvc.perform(post(RUTA).with(soloVer)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\"}".formatted(unico("Sin permiso"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void elCatalogoNoSeFiltraPorLaIglesiaDelToken() throws Exception {
		long id = idDe(crear(unico("Iglesia Visible")).andExpect(status().isCreated()));
		JwtRequestPostProcessor dentroDeOtraIglesia = jwt()
			.jwt(token -> token.subject(ID_ADMIN_SISTEMA).claim(ClaimsJwt.IGLESIA_ID, id + 1000))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_VER));

		mvc.perform(get(RUTA).with(dentroDeOtraIglesia))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", hasItems((int) id)));
		assertThat(jdbc.queryForObject("select relrowsecurity from pg_class where relname = 'iglesia'", Boolean.class))
			.isFalse();
	}

	private ResultActions crear(String nombre) throws Exception {
		return mvc.perform(post(RUTA).with(administradorDelSistema())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\"}".formatted(nombre)));
	}

	private ResultActions renombrar(long id, String nombre) throws Exception {
		return mvc.perform(put(RUTA + "/{id}", id).with(administradorDelSistema())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\"}".formatted(nombre)));
	}

	private static long idDe(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

	/** Las pruebas comparten base: cada nombre lleva un sufijo propio para no chocar con la unicidad. */
	private static String unico(String nombre) {
		return nombre + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private static JwtRequestPostProcessor administradorDelSistema() {
		return jwt().jwt(token -> token.subject(ID_ADMIN_SISTEMA))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_VER),
					new SimpleGrantedAuthority(Permisos.IGLESIAS_ADMINISTRAR));
	}

}
