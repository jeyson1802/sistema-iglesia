package com.iglesia.api.miembros.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.estadisticas.DatosEstadisticas;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Estadísticas de miembros: toda la iglesia para el administrador, solo su red para el líder, y aislamiento. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasMiembrosIntegrationTest {

	private static final String RUTA = "/api/estadisticas/miembros";

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void elAdministradorVeTodaLaIglesiaYElLiderSoloSuRed() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		DatosEstadisticas datos = new DatosEstadisticas(mvc, jdbc, iglesia);
		long pastor = datos.miembro("Pastor", null, HOY.minusYears(3));
		long ana = datos.miembro("Ana", pastor, HOY.minusYears(2));
		long beto = datos.miembro("Beto", ana, HOY);
		datos.miembro("Carla", beto, HOY);
		datos.miembro("Dani", pastor, HOY);

		mvc.perform(get(RUTA).param("periodo", "ANIO").with(datos.admin()))
			.andExpect(status().isOk())
			.andExpect(header().exists("X-Trace-Id"))
			.andExpect(jsonPath("$.red").value(false))
			.andExpect(jsonPath("$.activos").value(5))
			.andExpect(jsonPath("$.altas").value(3))
			.andExpect(jsonPath("$.directos", nullValue()))
			.andExpect(jsonPath("$.sinLider", notNullValue()))
			.andExpect(jsonPath("$.periodo.granularidad").value("MES"))
			.andExpect(jsonPath("$.serie", hasSize(HOY.getMonthValue())))
			.andExpect(jsonPath("$.porCondicion[?(@.clave == 'NUEVO')].valor").value(5))
			.andExpect(jsonPath("$.porEdad[?(@.clave == 'SIN_DATO')].valor").value(5));

		JwtRequestPostProcessor liderAna = PruebasNegocio.liderDe(iglesia, ana);
		mvc.perform(get(RUTA).param("periodo", "ANIO").with(liderAna))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.red").value(true))
			.andExpect(jsonPath("$.activos").value(2))
			.andExpect(jsonPath("$.directos").value(1))
			.andExpect(jsonPath("$.sinLider", nullValue()));
	}

	@Test
	void otraIglesiaNoVeLosMiembrosYUnMiembroNoTieneEstadisticas() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		long ana = new DatosEstadisticas(mvc, jdbc, iglesia).miembro("Ana", null, HOY);

		mvc.perform(get(RUTA).with(PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activos").value(0));
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("MIEMBRO"), ana, Permisos.MIEMBROS_VER)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.traceId", notNullValue()));
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null)))
			.andExpect(status().isForbidden());
		mvc.perform(get(RUTA).param("periodo", "SIGLO").with(PruebasNegocio.administradorDe(iglesia)))
			.andExpect(status().isBadRequest());
		mvc.perform(get(RUTA).param("periodo", "PERSONALIZADO").param("desde", "2026-10-01")
				.with(PruebasNegocio.administradorDe(iglesia)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.hasta", notNullValue()));
	}

}
