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

/** Regla de ocasiones (días de culto y grupo), alertas, ámbito del líder y aislamiento del seguimiento. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SeguimientoIntegrationTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void dosServiciosElMismoDiaCuentanComoUnoYLoDeHoyNoEsFaltaTodavia() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana", null);
		long primero = culto(admin, "Primer servicio");
		long segundo = culto(admin, "Segundo servicio");
		LocalDate ayer = HOY.minusDays(1);
		marcar(admin, abrir(admin, primero, ayer), ana);
		abrir(admin, segundo, ayer);
		abrir(admin, primero, HOY);

		mvc.perform(get("/api/miembros/{id}/asistencia", ana).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reunionesQueCuentan").value(1))
			.andExpect(jsonPath("$.porcentaje").value(100))
			.andExpect(jsonPath("$.faltasSeguidas").value(0));
		mvc.perform(get("/api/miembros/{id}/asistencia/ocasiones", ana).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].estado").value("ASISTIO"))
			.andExpect(jsonPath("$.content[0].asistioA").value("Primer servicio"));
	}

	@Test
	void lasFaltasSeguidasGeneranAlertaSegunLasReglasDeLaIglesia() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long luis = miembro(admin, "Luis", null);
		long culto = culto(admin, "Dominical");
		marcar(admin, abrir(admin, culto, HOY.minusDays(21)), luis);
		abrir(admin, culto, HOY.minusDays(14));
		abrir(admin, culto, HOY.minusDays(7));

		mvc.perform(get("/api/seguimiento-asistencia").with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(put("/api/configuracion-seguimiento").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"faltasSeguidas\":2,\"puntosCaida\":30,\"diasNuevo\":90}")).andExpect(status().isOk());
		mvc.perform(get("/api/seguimiento-asistencia").param("alerta", "FALTAS_SEGUIDAS").with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].miembroId").value(luis))
			.andExpect(jsonPath("$.content[0].faltasSeguidas").value(2));
		mvc.perform(put("/api/configuracion-seguimiento").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"faltasSeguidas\":1,\"puntosCaida\":30,\"diasNuevo\":90}")).andExpect(status().isBadRequest());
	}

	@Test
	void elLiderSoloSigueASuRedYLaOtraIglesiaNoVeNada() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long lider = miembro(admin, "Marta", null);
		long discipulo = miembro(admin, "Beto", lider);
		long otro = miembro(admin, "Carla", null);
		JwtRequestPostProcessor liderToken = PruebasNegocio.token(iglesia, List.of("LIDER"), lider,
				Permisos.MIEMBROS_VER, Permisos.ASISTENCIA_GRUPO);

		mvc.perform(get("/api/seguimiento-asistencia").param("todos", "true").with(liderToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].miembroId").value(discipulo));
		mvc.perform(get("/api/miembros/{id}/asistencia/ocasiones", otro).with(liderToken))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/seguimiento-asistencia/servicios").with(liderToken)).andExpect(status().isForbidden());

		JwtRequestPostProcessor otraIglesia = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		mvc.perform(get("/api/seguimiento-asistencia").param("todos", "true").with(otraIglesia))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(get("/api/miembros/{id}/asistencia", discipulo).with(otraIglesia)).andExpect(status().isNotFound());
	}

	private long miembro(JwtRequestPostProcessor admin, String nombres, Long lider) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\",\"fechaIngreso\":\"%s\",\"liderId\":%s}"
				.formatted(nombres, HOY.minusYears(1), lider))));
	}

	private long culto(JwtRequestPostProcessor admin, String nombre) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/cultos-programados").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"diaSemana\":\"DOMINGO\",\"hora\":\"10:00\"}".formatted(nombre))));
	}

	private long abrir(JwtRequestPostProcessor admin, long culto, LocalDate fecha) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/reuniones").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"cultoProgramadoId\":%d,\"fecha\":\"%s\"}".formatted(culto, fecha))));
	}

	private void marcar(JwtRequestPostProcessor admin, long reunion, long miembro) throws Exception {
		mvc.perform(post("/api/reuniones/{id}/asistencias", reunion).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"miembroId\":%d}".formatted(miembro))).andExpect(status().isCreated());
	}

}
