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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo de iglesias contra PostgreSQL real: alta y edición con la identidad, unicidad del nombre comercial,
 * filtros paginados, apariencia, logo de cualquier iglesia y permisos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class IglesiaIntegrationTest {

	private static final String RUTA = "/api/iglesias";

	private static final String ID_ADMIN_SISTEMA = "7";

	private static final String SAN_JUAN_DE_LURIGANCHO = "150132";

	private static final String CUSCO = "080101";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void daDeAltaUnaIglesiaConSuIdentidadYLaRegistraSinSusDatos(CapturedOutput salida) throws Exception {
		String nombre = unico("Iglesia Central");

		String creada = crear(cuerpo("  " + nombre + "  ", SAN_JUAN_DE_LURIGANCHO, "\"contacto@central.org\""))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, containsString(RUTA + "/")))
			.andExpect(jsonPath("$.nombreComercial").value(nombre))
			.andExpect(jsonPath("$.denominacion").value("Denominación de " + nombre))
			.andExpect(jsonPath("$.ubicacion.distrito.nombre").value("SAN JUAN DE LURIGANCHO"))
			.andExpect(jsonPath("$.ubicacion.departamento.nombre").value("LIMA"))
			.andExpect(jsonPath("$.correo").value("contacto@central.org"))
			.andExpect(jsonPath("$.telefono").doesNotExist())
			.andExpect(jsonPath("$.tieneLogo").value(false))
			.andExpect(jsonPath("$.apariencia.preset").value("Aura"))
			.andExpect(jsonPath("$.apariencia.tipografia").value("Playfair Display"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(creada, "$.id")).longValue();

		mvc.perform(get(RUTA + "/{id}", id).with(administradorDelSistema()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreComercial").value(nombre));
		assertThat(salida.getAll()).contains("Iglesia creada id=" + id)
			.doesNotContain(nombre)
			.doesNotContain("contacto@central.org");
	}

	@Test
	void filtraElCatalogoPorTextoUbigeoYEstadoConPaginacion() throws Exception {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		crear(cuerpo("Betel Cusco " + sufijo, CUSCO, null)).andExpect(status().isCreated());
		crear(cuerpo("Zion Lima " + sufijo, SAN_JUAN_DE_LURIGANCHO, null)).andExpect(status().isCreated());

		mvc.perform(get(RUTA).param("q", sufijo).param("ubigeo", "08").with(administradorDelSistema()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].nombreComercial").value("Betel Cusco " + sufijo));
		String pagina = mvc.perform(get(RUTA).param("q", sufijo).param("activa", "true").with(administradorDelSistema()))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();
		List<String> nombres = JsonPath.read(pagina, "$.content[*].nombreComercial");
		assertThat(nombres).containsExactly("Betel Cusco " + sufijo, "Zion Lima " + sufijo);
		mvc.perform(get(RUTA).param("ubigeo", "1").with(administradorDelSistema()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void elNombreComercialEsUnicoSinDistinguirMayusculas() throws Exception {
		String nombre = unico("Iglesia Sion");
		crear(cuerpo(nombre, CUSCO, null)).andExpect(status().isCreated());
		long otra = idDe(crear(cuerpo(unico("Otra"), CUSCO, null)));

		crear(cuerpo(nombre.toUpperCase(), CUSCO, null)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.nombreComercial").value("Ya existe una iglesia con ese nombre comercial"))
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		actualizar(otra, cuerpo(nombre.toLowerCase(), CUSCO, null)).andExpect(status().isConflict());
	}

	@Test
	void editaLaIdentidadIncluidoCambiarSoloLasMayusculas() throws Exception {
		String nombre = unico("Iglesia Norte");
		long id = idDe(crear(cuerpo(nombre, CUSCO, null)));

		actualizar(id, cuerpo(nombre.toUpperCase(), SAN_JUAN_DE_LURIGANCHO, "\"norte@iglesia.org\""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreComercial").value(nombre.toUpperCase()))
			.andExpect(jsonPath("$.distritoCodigo").value(SAN_JUAN_DE_LURIGANCHO));
		actualizar(999_999L, cuerpo(unico("Nadie"), CUSCO, null)).andExpect(status().isNotFound());
	}

	@Test
	void validaLosDatos() throws Exception {
		crear(cuerpo("x", CUSCO, null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.nombreComercial").exists());
		crear(cuerpo("x".repeat(61), CUSCO, null)).andExpect(status().isBadRequest());
		crear(cuerpo(unico("Sin distrito"), "999999", null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.distritoCodigo").exists());
		crear(cuerpo(unico("Correo"), CUSCO, "\"no-es-correo\"")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.correo").exists());
		mvc.perform(post(RUTA).with(administradorDelSistema()).contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.denominacion").exists())
			.andExpect(jsonPath("$.errores.direccion").exists());
	}

	@Test
	void cambiaLaAparienciaSoloConValoresAdmitidos() throws Exception {
		long id = idDe(crear(cuerpo(unico("Iglesia Color"), CUSCO, null)));

		apariencia(id, "Lara", "indigo", "\"zinc\"", "Cinzel").andExpect(status().isOk())
			.andExpect(jsonPath("$.preset").value("Lara"))
			.andExpect(jsonPath("$.colorSuperficie").value("zinc"))
			.andExpect(jsonPath("$.tipografia").value("Cinzel"));
		apariencia(id, "Material", "indigo", "null", "Cinzel").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.preset").exists());
		apariencia(id, "Aura", "marron", "null", "Cinzel").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.colorPrimario").exists());
		apariencia(id, "Aura", "emerald", "null", "Comic Sans").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.tipografia").exists());
		mvc.perform(get(RUTA + "/{id}", id).with(administradorDelSistema()))
			.andExpect(jsonPath("$.apariencia.colorPrimario").value("indigo"));
	}

	@Test
	void subeSirveYQuitaElLogoDeCualquierIglesia() throws Exception {
		long id = idDe(crear(cuerpo(unico("Iglesia Logo"), CUSCO, null)));

		mvc.perform(put(RUTA + "/{id}/logo", id).with(administradorDelSistema())
			.contentType(MediaType.IMAGE_PNG)
			.content(ImagenesPrueba.PNG)).andExpect(status().isNoContent());
		String etiqueta = mvc.perform(get(RUTA + "/{id}/logo", id).with(administradorDelSistema()))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
			.andReturn()
			.getResponse()
			.getHeader(HttpHeaders.ETAG);
		mvc.perform(get(RUTA + "/{id}/logo", id).with(administradorDelSistema()).header(HttpHeaders.IF_NONE_MATCH,
				etiqueta))
			.andExpect(status().isNotModified());
		mvc.perform(get(RUTA + "/{id}", id).with(administradorDelSistema()))
			.andExpect(jsonPath("$.tieneLogo").value(true));

		mvc.perform(delete(RUTA + "/{id}/logo", id).with(administradorDelSistema())).andExpect(status().isNoContent());
		mvc.perform(get(RUTA + "/{id}/logo", id).with(administradorDelSistema())).andExpect(status().isNotFound());
	}

	@Test
	void rechazaLogosQueNoSonImagenesAdmitidasOSonDemasiadoGrandes() throws Exception {
		long id = idDe(crear(cuerpo(unico("Iglesia Archivo"), CUSCO, null)));

		mvc.perform(put(RUTA + "/{id}/logo", id).with(administradorDelSistema())
			.contentType(MediaType.IMAGE_PNG)
			.content(ImagenesPrueba.SVG))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.logo").value("El archivo no es una imagen PNG, JPEG o WebP"));
		mvc.perform(put(RUTA + "/{id}/logo", id).with(administradorDelSistema())
			.contentType(MediaType.IMAGE_PNG)
			.content(ImagenesPrueba.pngDe(1024 * 1024 + 1)))
			.andExpect(status().is(413))
			.andExpect(jsonPath("$.traceId").isNotEmpty());
	}

	@Test
	void exigeElPermisoDeCadaAccionYUnToken() throws Exception {
		JwtRequestPostProcessor soloVer = jwt().authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_VER));
		JwtRequestPostProcessor adminIglesia = jwt().jwt(token -> token.claim(ClaimsJwt.IGLESIA_ID, 1L))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIA_PERSONALIZAR));

		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA).with(adminIglesia))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.traceId").isNotEmpty());
		mvc.perform(get(RUTA).with(soloVer)).andExpect(status().isOk());
		mvc.perform(post(RUTA).with(soloVer)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo(unico("Sin permiso"), CUSCO, null))).andExpect(status().isForbidden());
		mvc.perform(put(RUTA + "/1/apariencia").with(adminIglesia)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"preset\":\"Aura\",\"colorPrimario\":\"emerald\",\"tipografia\":\"Cinzel\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void elCatalogoNoSeFiltraPorLaIglesiaDelToken() throws Exception {
		String nombre = unico("Iglesia Visible");
		long id = idDe(crear(cuerpo(nombre, CUSCO, null)));
		JwtRequestPostProcessor dentroDeOtraIglesia = jwt()
			.jwt(token -> token.subject(ID_ADMIN_SISTEMA).claim(ClaimsJwt.IGLESIA_ID, id + 1000))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_VER));

		mvc.perform(get(RUTA).param("q", nombre).with(dentroDeOtraIglesia))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItems((int) id)));
		assertThat(jdbc.queryForObject("select relrowsecurity from pg_class where relname = 'iglesia'", Boolean.class))
			.isFalse();
		assertThat(jdbc.queryForObject("select relrowsecurity from pg_class where relname = 'logo_iglesia'",
				Boolean.class)).isTrue();
	}

	private ResultActions crear(String cuerpo) throws Exception {
		return mvc.perform(post(RUTA).with(administradorDelSistema()).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private ResultActions actualizar(long id, String cuerpo) throws Exception {
		return mvc.perform(put(RUTA + "/{id}", id).with(administradorDelSistema())
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo));
	}

	private ResultActions apariencia(long id, String preset, String color, String superficie, String tipografia)
			throws Exception {
		return mvc.perform(put(RUTA + "/{id}/apariencia", id).with(administradorDelSistema())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"preset":"%s","colorPrimario":"%s","colorSuperficie":%s,"tipografia":"%s"}"""
				.formatted(preset, color, superficie, tipografia)));
	}

	/** Cuerpo válido con el nombre, el distrito y el correo indicados (el correo ya entre comillas o null). */
	static String cuerpo(String nombre, String distrito, String correo) {
		return """
				{"nombreComercial":"%s","denominacion":"Denominación de %s","direccion":"Av. Principal 123",\
				"distritoCodigo":"%s","correo":%s}""".formatted(nombre, nombre.strip(), distrito, correo);
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
