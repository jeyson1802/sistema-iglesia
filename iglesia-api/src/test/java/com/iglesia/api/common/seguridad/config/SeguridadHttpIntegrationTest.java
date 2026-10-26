package com.iglesia.api.common.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadHttpIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void laApiExigeToken() throws Exception {
		mvc.perform(get("/api/miembros")).andExpect(status().isUnauthorized());
	}

	@Test
	void laSaludEsPublica() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void corsSoloPermiteLosOrigenesDelFrontend() throws Exception {
		mvc.perform(options("/api/miembros").header(HttpHeaders.ORIGIN, "http://localhost:4200")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

		mvc.perform(options("/api/miembros").header(HttpHeaders.ORIGIN, "http://malicioso.test")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden());
	}

}
