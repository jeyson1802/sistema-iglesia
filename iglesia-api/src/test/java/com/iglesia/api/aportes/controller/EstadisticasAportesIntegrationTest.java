package com.iglesia.api.aportes.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regla de aportes: solo con {@code aportes.ver} y alcance de toda la iglesia; un líder nunca los ve en su inicio. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasAportesIntegrationTest {

	private static final String RUTA = "/api/estadisticas/aportes";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void elAdministradorVeLosTotalesYQuienNoTienePermisoODeIglesiaNo() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long categoria = PruebasNegocio.idDe(mvc.perform(post("/api/categorias-aporte").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Ofrenda de prueba\",\"exigeAportante\":false}")));
		mvc.perform(post("/api/aportes").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fecha\":\"%s\",\"categoriaId\":%d,\"monto\":150.50,\"medio\":\"EFECTIVO\"}"
				.formatted(LocalDate.now(), categoria)))
			.andExpect(status().isCreated());

		mvc.perform(get(RUTA).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resumen.total").value(150.50))
			.andExpect(jsonPath("$.resumen.cantidad").value(1))
			.andExpect(jsonPath("$.serie[-1].cantidad").value(1));
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("LIDER"), 5L, Permisos.APORTES_VER)))
			.andExpect(status().isForbidden());
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null,
				Permisos.MIEMBROS_VER)))
			.andExpect(status().isForbidden());
	}

}
