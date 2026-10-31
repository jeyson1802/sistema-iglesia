package com.iglesia.api.grupos.controller;

import java.time.LocalDate;
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

/** Estadísticas de grupos: asistencia de los integrantes, grupos sin reuniones y ámbito del líder. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasGruposIntegrationTest {

	private static final String RUTA = "/api/estadisticas/grupos";

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void calculaLaAsistenciaDeLosIntegrantesYElLiderSoloVeLosGruposDeSuRed() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		DatosEstadisticas datos = new DatosEstadisticas(mvc, jdbc, iglesia);
		long pastor = datos.miembro("Pastor", null, HOY.minusYears(3));
		long ana = datos.miembro("Ana", pastor, HOY.minusYears(1));
		long luis = datos.miembro("Luis", pastor, HOY.minusYears(1));
		long grupoAna = datos.grupo(ana);
		datos.grupo(luis);
		long beto = datos.miembro("Beto", ana, HOY.minusYears(1));
		long carla = datos.miembro("Carla", ana, HOY.minusYears(1));
		datos.integrar(grupoAna, beto);
		datos.integrar(grupoAna, carla);
		long reunion = datos.reunionDeGrupo(grupoAna, HOY);
		datos.marcar(reunion, beto);

		mvc.perform(get(RUTA).with(datos.admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.grupos").value(2))
			.andExpect(jsonPath("$.reuniones").value(1))
			.andExpect(jsonPath("$.sinReuniones").value(1))
			.andExpect(jsonPath("$.porcentajeAsistencia").value(50))
			.andExpect(jsonPath("$.miembrosEnGrupo").value(2))
			.andExpect(jsonPath("$.porGrupo[0].grupoId").value(grupoAna))
			.andExpect(jsonPath("$.porGrupo[0].porcentaje").value(50));

		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("LIDER"), ana, Permisos.ASISTENCIA_GRUPO)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.red").value(true))
			.andExpect(jsonPath("$.grupos").value(1))
			.andExpect(jsonPath("$.porGrupo[0].grupoId").value(grupoAna));
		mvc.perform(get(RUTA).with(PruebasNegocio.liderDe(iglesia, ana))).andExpect(status().isForbidden());
	}

}
