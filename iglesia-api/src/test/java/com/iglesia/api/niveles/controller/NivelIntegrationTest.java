package com.iglesia.api.niveles.controller;

import java.util.List;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catálogo de niveles por iglesia contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class NivelIntegrationTest {

	private static final String RUTA = "/api/niveles";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void creaNivelesAlFinalDelOrdenYLosRegistraSinSuNombre(CapturedOutput salida) throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));

		crear(admin, "Pastor").andExpect(status().isCreated()).andExpect(jsonPath("$.orden").value(1));
		long diacono = PruebasNegocio.idDe(crear(admin, "  Diácono ").andExpect(jsonPath("$.orden").value(2)));

		mvc.perform(get(RUTA).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("Pastor", "Diácono")));
		assertThat(salida.getOut()).contains("Nivel creado id=" + diacono).doesNotContain("Diácono");
	}

	@Test
	void unNombreRepetidoSinDistinguirMayusculasEsUnConflicto() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		crear(admin, "Líder").andExpect(status().isCreated());

		crear(admin, "LÍDER").andExpect(status().isConflict()).andExpect(jsonPath("$.errores.nombre").exists());
	}

	@Test
	void elMismoNombreSePuedeUsarEnOtraIglesia() throws Exception {
		crear(PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc)), "Diácono")
			.andExpect(status().isCreated());
		crear(PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc)), "Diácono")
			.andExpect(status().isCreated());
	}

	@Test
	void reordenaConTodosLosNivelesYRechazaUnaListaIncompleta() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long a = PruebasNegocio.idDe(crear(admin, "A"));
		long b = PruebasNegocio.idDe(crear(admin, "B"));

		ordenar(admin, List.of(a)).andExpect(status().isBadRequest());
		ordenar(admin, List.of(b, a)).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", contains("B", "A")));
	}

	@Test
	void desactivaYReactivaUnNivel() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long id = PruebasNegocio.idDe(crear(admin, "Antiguo"));

		estado(admin, id, false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
		estado(admin, id, true).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
	}

	@Test
	void unNivelDeOtraIglesiaNoExiste() throws Exception {
		long ajeno = PruebasNegocio.idDe(crear(PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc)), "X"));
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));

		estado(admin, ajeno, false).andExpect(status().isNotFound());
		mvc.perform(put(RUTA + "/{id}", ajeno).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Y\"}")).andExpect(status().isNotFound());
	}

	@Test
	void quienSoloVeMiembrosConsultaPeroNoModifica() throws Exception {
		JwtRequestPostProcessor lector = PruebasNegocio.token(PruebasNegocio.nuevaIglesia(jdbc), List.of("LIDER"), null,
				Permisos.MIEMBROS_VER);

		mvc.perform(get(RUTA).with(lector)).andExpect(status().isOk());
		crear(lector, "Otro").andExpect(status().isForbidden());
	}

	private ResultActions crear(JwtRequestPostProcessor token, String nombre) throws Exception {
		return mvc.perform(post(RUTA).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\"}".formatted(nombre)));
	}

	private ResultActions ordenar(JwtRequestPostProcessor token, List<Long> ids) throws Exception {
		return mvc.perform(put(RUTA + "/orden").with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":" + ids + "}"));
	}

	private ResultActions estado(JwtRequestPostProcessor token, long id, boolean activo) throws Exception {
		return mvc.perform(patch(RUTA + "/{id}/estado", id).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":" + activo + "}"));
	}

}
