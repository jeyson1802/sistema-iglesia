package com.iglesia.api.aportes.controller;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Moneda y categorías de aporte por iglesia contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ConfiguracionAportesIntegrationTest {

	private static final String CATEGORIAS = "/api/categorias-aporte";

	private static final String CONFIGURACION = "/api/aportes/configuracion";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unaIglesiaNuevaNaceConDiezmoYOfrenda() throws Exception {
		JwtRequestPostProcessor sistema = jwt().jwt(token -> token.subject("1"))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_ADMINISTRAR));
		long iglesia = PruebasNegocio.idDe(mvc.perform(post("/api/iglesias").with(sistema)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Aportes %s\"}".formatted(System.nanoTime()))).andExpect(status().isCreated()));

		mvc.perform(get(CATEGORIAS).with(PruebasNegocio.administradorDe(iglesia)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("Diezmo", "Ofrenda")))
			.andExpect(jsonPath("$[*].exigeAportante", contains(true, false)));
	}

	@Test
	void administraCategoriasSinRegistrarSuNombre(CapturedOutput salida) throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long misiones = PruebasNegocio.idDe(crear(admin, "Misiones", false).andExpect(status().isCreated())
			.andExpect(jsonPath("$.orden").value(1)));
		long templo = PruebasNegocio.idDe(crear(admin, "Pro templo", true));

		crear(admin, "MISIONES", false).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.nombre").exists());
		mvc.perform(put(CATEGORIAS + "/orden").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[%d,%d]}".formatted(templo, misiones)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("Pro templo", "Misiones")));
		mvc.perform(patch(CATEGORIAS + "/{id}/estado", misiones).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
		assertThat(salida.getOut()).contains("Categoría de aporte creada id=" + misiones).doesNotContain("Misiones");
	}

	@Test
	void laMonedaSeCambiaSoloMientrasNoHayAportesVigentes() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));

		mvc.perform(get(CONFIGURACION).with(admin))
			.andExpect(jsonPath("$.moneda").value("PEN"))
			.andExpect(jsonPath("$.monedaEditable").value(true));
		moneda(admin, "JPY").andExpect(status().isBadRequest());
		moneda(admin, "USD").andExpect(status().isOk()).andExpect(jsonPath("$.moneda").value("USD"));

		long categoria = PruebasNegocio.idDe(crear(admin, "Ofrenda", false));
		mvc.perform(post("/api/aportes").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fecha\":\"2026-01-04\",\"categoriaId\":%d,\"monto\":10,\"medio\":\"EFECTIVO\"}"
				.formatted(categoria))).andExpect(status().isCreated());

		mvc.perform(get(CONFIGURACION).with(admin)).andExpect(jsonPath("$.monedaEditable").value(false));
		moneda(admin, "EUR").andExpect(status().isConflict());
	}

	@Test
	void quienRegistraConsultaCategoriasPeroNoLasModificaNiVeLasMonedas() throws Exception {
		JwtRequestPostProcessor cajero = PruebasNegocio.token(PruebasNegocio.nuevaIglesia(jdbc), List.of("TESORERO"),
				null, Permisos.APORTES_REGISTRAR);

		mvc.perform(get(CATEGORIAS).with(cajero)).andExpect(status().isOk());
		mvc.perform(get(CONFIGURACION).with(cajero)).andExpect(status().isOk());
		mvc.perform(get("/api/aportes/monedas").with(cajero)).andExpect(status().isForbidden());
		crear(cajero, "Otra", false).andExpect(status().isForbidden());
	}

	@Test
	void lasCategoriasYLaMonedaSonDeCadaIglesia() throws Exception {
		JwtRequestPostProcessor otra = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long ajena = PruebasNegocio.idDe(crear(otra, "Ajena", false));
		moneda(otra, "USD").andExpect(status().isOk());
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));

		crear(admin, "Ajena", false).andExpect(status().isCreated());
		mvc.perform(get(CONFIGURACION).with(admin)).andExpect(jsonPath("$.moneda").value("PEN"));
		mvc.perform(patch(CATEGORIAS + "/{id}/estado", ajena).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isNotFound());
	}

	private ResultActions crear(JwtRequestPostProcessor token, String nombre, boolean exigeAportante)
			throws Exception {
		return mvc.perform(post(CATEGORIAS).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"exigeAportante\":%s}".formatted(nombre, exigeAportante)));
	}

	private ResultActions moneda(JwtRequestPostProcessor token, String codigo) throws Exception {
		return mvc.perform(put(CONFIGURACION).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"moneda\":\"%s\"}".formatted(codigo)));
	}

}
