package com.iglesia.api.asistencia.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Resumen y detalle por reunión de la asistencia de un grupo, con el ámbito del líder y el aislamiento. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AsistenciaGrupoIntegrationTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cadaReunionDiceQuienesAsistieronFaltaronYVinieronDeVisita() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana", HOY.minusYears(1));
		long grupo = grupo(admin, ana);
		long beto = integrante(admin, grupo, "Beto", HOY.minusYears(1));
		integrante(admin, grupo, "Carla", HOY.minusYears(1));
		integrante(admin, grupo, "Dani", HOY);
		long eva = miembro(admin, "Eva", HOY.minusYears(1));
		long anterior = abrir(admin, grupo, HOY.minusDays(14));
		long reciente = abrir(admin, grupo, HOY.minusDays(7));
		marcar(admin, reciente, beto);
		marcar(admin, reciente, eva);

		mvc.perform(get("/api/grupos/{id}/reuniones", grupo).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].reunionId").value(reciente))
			.andExpect(jsonPath("$.content[0].esperados").value(2))
			.andExpect(jsonPath("$.content[0].asistieron").value(1))
			.andExpect(jsonPath("$.content[0].faltaron").value(1))
			.andExpect(jsonPath("$.content[0].visitantes").value(1))
			.andExpect(jsonPath("$.content[0].porcentaje").value(50))
			.andExpect(jsonPath("$.content[1].reunionId").value(anterior))
			.andExpect(jsonPath("$.content[1].porcentaje").value(0));
		mvc.perform(get("/api/grupos/{id}/reuniones/{reunionId}", grupo, reciente).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.asistieron[0].nombreCompleto").value("Beto Prueba"))
			.andExpect(jsonPath("$.faltaron.length()").value(1))
			.andExpect(jsonPath("$.faltaron[0].nombreCompleto").value("Carla Prueba"))
			.andExpect(jsonPath("$.visitantes[0].miembroId").value(eva));
	}

	@Test
	void elLiderSoloVeSuGrupoYOtraIglesiaNoVeNada() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana", HOY.minusYears(1));
		long luis = miembro(admin, "Luis", HOY.minusYears(1));
		long grupoAna = grupo(admin, ana);
		long grupoLuis = grupo(admin, luis);
		long reunionLuis = abrir(admin, grupoLuis, HOY.minusDays(7));
		JwtRequestPostProcessor liderAna = PruebasNegocio.token(iglesia, List.of("LIDER"), ana, Permisos.MIEMBROS_VER,
				Permisos.ASISTENCIA_GRUPO);

		mvc.perform(get("/api/grupos/{id}/reuniones", grupoAna).with(liderAna)).andExpect(status().isOk());
		mvc.perform(get("/api/grupos/{id}/reuniones", grupoLuis).with(liderAna)).andExpect(status().isNotFound());
		mvc.perform(get("/api/grupos/{id}/reuniones/{reunionId}", grupoAna, reunionLuis).with(admin))
			.andExpect(status().isNotFound());

		JwtRequestPostProcessor otraIglesia = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		mvc.perform(get("/api/grupos/{id}/reuniones", grupoLuis).with(otraIglesia)).andExpect(status().isNotFound());
		mvc.perform(get("/api/grupos/{id}/reuniones/{reunionId}", grupoLuis, reunionLuis).with(otraIglesia))
			.andExpect(status().isNotFound());
		JwtRequestPostProcessor sinAsistencia = PruebasNegocio.token(iglesia, List.of("LIDER"), ana,
				Permisos.MIEMBROS_VER);
		mvc.perform(get("/api/grupos/{id}/reuniones", grupoAna).with(sinAsistencia)).andExpect(status().isForbidden());
	}

	private long miembro(JwtRequestPostProcessor admin, String nombres, LocalDate ingreso) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\",\"fechaIngreso\":\"%s\"}".formatted(nombres, ingreso))));
	}

	private long integrante(JwtRequestPostProcessor admin, long grupo, String nombres, LocalDate ingreso)
			throws Exception {
		long id = miembro(admin, nombres, ingreso);
		mvc.perform(put("/api/grupos/{id}/miembros/{miembroId}", grupo, id).with(admin))
			.andExpect(status().is2xxSuccessful());
		return id;
	}

	private long grupo(JwtRequestPostProcessor admin, long lider) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/grupos").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":%d}".formatted(lider))));
	}

	private long abrir(JwtRequestPostProcessor admin, long grupo, LocalDate fecha) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/reuniones").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"grupoId\":%d,\"fecha\":\"%s\"}".formatted(grupo, fecha))));
	}

	private void marcar(JwtRequestPostProcessor admin, long reunion, long miembro) throws Exception {
		mvc.perform(post("/api/reuniones/{id}/asistencias", reunion).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"miembroId\":%d}".formatted(miembro))).andExpect(status().isCreated());
	}

}
