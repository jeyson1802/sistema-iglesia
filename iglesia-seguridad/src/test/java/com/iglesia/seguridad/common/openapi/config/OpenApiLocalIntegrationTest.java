package com.iglesia.seguridad.common.openapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.seguridad.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con el perfil local, la especificación OpenAPI y Swagger UI se publican sin pedir token. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiLocalIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void laEspecificacionEsPublicaYDeclaraElEsquemaBearer() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").isNotEmpty())
			.andExpect(jsonPath("$.info.description").isNotEmpty())
			.andExpect(jsonPath("$.components.securitySchemes.%s.type", OpenApiConfig.ESQUEMA_SEGURIDAD).value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.%s.scheme", OpenApiConfig.ESQUEMA_SEGURIDAD)
				.value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.%s.bearerFormat", OpenApiConfig.ESQUEMA_SEGURIDAD)
				.value("JWT"))
			.andExpect(jsonPath("$.security[0].%s", OpenApiConfig.ESQUEMA_SEGURIDAD).exists());
	}

	@Test
	void swaggerUiEsPublico() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
