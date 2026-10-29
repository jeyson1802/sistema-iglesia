package com.iglesia.api.ubigeo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.TestcontainersConfiguration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ubigeo INEI cargado por V4, contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class UbigeoIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void devuelveLos25DepartamentosOrdenadosPorNombre() throws Exception {
		mvc.perform(get("/api/ubigeo/departamentos").with(jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(25))
			.andExpect(jsonPath("$[0].nombre").value("AMAZONAS"))
			.andExpect(jsonPath("$[*].codigo", hasItem("15")));
	}

	@Test
	void devuelveLasProvinciasYLosDistritosEnCascada() throws Exception {
		mvc.perform(get("/api/ubigeo/departamentos/15/provincias").with(jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].codigo", hasItem("1501")));
		mvc.perform(get("/api/ubigeo/provincias/1501/distritos").with(jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(44))
			.andExpect(jsonPath("$[*].codigo", hasItem("150101")));
	}

	@Test
	void unCodigoInexistenteResponde404ConTraceId() throws Exception {
		mvc.perform(get("/api/ubigeo/provincias/9999/distritos").with(jwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.traceId").isNotEmpty());
	}

	@Test
	void exigeEstarAutenticado() throws Exception {
		mvc.perform(get("/api/ubigeo/departamentos")).andExpect(status().isUnauthorized());
	}

}
