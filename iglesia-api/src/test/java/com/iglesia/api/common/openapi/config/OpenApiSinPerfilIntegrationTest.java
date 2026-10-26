package com.iglesia.api.common.openapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sin perfil (configuración de producción), la documentación no existe: responde 404, no 401. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSinPerfilIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void laEspecificacionNoSePublica() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
	}

	@Test
	void swaggerUiNoSePublica() throws Exception {
		mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
	}

}
