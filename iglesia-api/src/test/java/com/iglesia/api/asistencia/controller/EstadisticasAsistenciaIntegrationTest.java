package com.iglesia.api.asistencia.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import com.iglesia.api.common.estadisticas.DatosEstadisticas;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Estadísticas de asistencia: asistentes por culto, retorno de los nuevos y alcance del líder. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasAsistenciaIntegrationTest {

	private static final String RUTA = "/api/estadisticas/asistencia";

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void promediaLosAsistentesPorCultoYEnLaRedSoloCuentaALosDeElla() throws Exception {
		// 13 semanas completas (de lunes) hasta hoy: la serie va por semana
		String desde = HOY.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(12).toString();
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		DatosEstadisticas datos = new DatosEstadisticas(mvc, jdbc, iglesia);
		long pastor = datos.miembro("Pastor", null, HOY.minusYears(3));
		long ana = datos.miembro("Ana", pastor, HOY.minusYears(1));
		long beto = datos.miembro("Beto", ana, HOY.minusDays(1));
		long luis = datos.miembro("Luis", pastor, HOY.minusYears(1));
		long culto = datos.culto(HOY);
		datos.marcar(culto, ana);
		datos.marcar(culto, beto);
		datos.marcar(culto, luis);

		mvc.perform(get(RUTA).param("periodo", "PERSONALIZADO").param("desde", desde).param("hasta", HOY.toString())
				.with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null, Permisos.ASISTENCIA_VER)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cultos").value(1))
			.andExpect(jsonPath("$.asistentesPromedio").value(3.0))
			.andExpect(jsonPath("$.personasQueAsistieron").value(3))
			.andExpect(jsonPath("$.llegaron").value(1))
			.andExpect(jsonPath("$.volvieron").value(1))
			.andExpect(jsonPath("$.serie.length()").value(13))
			.andExpect(jsonPath("$.alertas.conAlertas").isNumber());

		mvc.perform(get(RUTA).param("periodo", "PERSONALIZADO").param("desde", desde).param("hasta", HOY.toString())
				.with(PruebasNegocio.token(iglesia, List.of("LIDER"), ana, Permisos.ASISTENCIA_GRUPO)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.red").value(true))
			.andExpect(jsonPath("$.asistentesPromedio").value(1.0))
			.andExpect(jsonPath("$.personasQueAsistieron").value(1));
	}

}
