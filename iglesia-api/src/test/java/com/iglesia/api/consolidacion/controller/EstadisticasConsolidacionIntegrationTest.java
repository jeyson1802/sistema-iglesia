package com.iglesia.api.consolidacion.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Estadísticas de consolidación: solo de toda la iglesia y con su permiso. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasConsolidacionIntegrationTest {

	private static final String RUTA = "/api/estadisticas/consolidacion";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void elCoordinadorVeLasCifrasYElLiderNo() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);

		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null,
				Permisos.CONSOLIDACION_VER)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rondasEnCurso").value(0))
			.andExpect(jsonPath("$.contactos").value(0))
			.andExpect(jsonPath("$.contactosPorMedio.length()").value(4))
			.andExpect(jsonPath("$.contactosPorMedio[0].etiqueta").value("Llamada"));
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("LIDER"), 5L, Permisos.CONSOLIDACION_VER)))
			.andExpect(status().isForbidden());
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null,
				Permisos.CONSOLIDACION_CONTACTAR)))
			.andExpect(status().isForbidden());
	}

}
