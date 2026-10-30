package com.iglesia.api.grupos.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Grupos (identificados por su líder), sus integrantes y el nombre configurable de los grupos contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GrupoIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void creaUnGrupoConLiderYCuentaSusMiembrosActivos() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long lider = miembro(admin, "{\"nombres\":\"Ana\",\"apellidos\":\"Lider\"}");

		long grupo = PruebasNegocio.idDe(crear(admin, """
				{"liderId":%d,"diaReunion":"JUEVES","horaReunion":"19:30","distritoCodigo":"150135"}
				""".formatted(lider)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.lider.nombre").value("Ana Lider"))
			.andExpect(jsonPath("$.nombre").doesNotExist())
			.andExpect(jsonPath("$.ubicacion.distrito.codigo").value("150135"))
			.andExpect(jsonPath("$.cantidadMiembros").value(0)));
		miembro(admin, "{\"nombres\":\"Luis\",\"apellidos\":\"Grupo\",\"grupoId\":%d}".formatted(grupo));

		mvc.perform(get("/api/grupos/{id}", grupo).with(admin)).andExpect(jsonPath("$.cantidadMiembros").value(1));
		mvc.perform(get("/api/miembros/{id}", lider).with(admin)).andExpect(jsonPath("$.grupo").doesNotExist());
	}

	@Test
	void elLiderEsObligatorioYTieneUnSoloGrupo() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long lider = miembro(admin, "{\"nombres\":\"Uno\",\"apellidos\":\"Lider\"}");
		long otro = miembro(admin, "{\"nombres\":\"Otro\",\"apellidos\":\"Lider\"}");
		long grupo = PruebasNegocio.idDe(crear(admin, "{\"liderId\":%d}".formatted(lider)));

		crear(admin, "{\"distritoCodigo\":\"150135\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.liderId").exists());
		crear(admin, "{\"liderId\":%d}".formatted(lider)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.liderId").exists());
		long grupoDeOtro = PruebasNegocio.idDe(crear(admin, "{\"liderId\":%d}".formatted(otro)));
		mvc.perform(put("/api/grupos/{id}", grupoDeOtro).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":%d}".formatted(lider))).andExpect(status().isConflict());
		mvc.perform(put("/api/grupos/{id}", grupo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":%d,\"diaReunion\":\"LUNES\"}".formatted(lider))).andExpect(status().isOk());
	}

	@Test
	void agregaMueveYQuitaIntegrantes() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long grupoA = PruebasNegocio.idDe(crear(admin,
				"{\"liderId\":%d}".formatted(miembro(admin, "{\"nombres\":\"Ana\",\"apellidos\":\"Alfa\"}"))));
		long grupoB = PruebasNegocio.idDe(crear(admin,
				"{\"liderId\":%d}".formatted(miembro(admin, "{\"nombres\":\"Beto\",\"apellidos\":\"Beta\"}"))));
		long rosa = miembro(admin, "{\"nombres\":\"Rosa\",\"apellidos\":\"Quispe\",\"grupoId\":%d}".formatted(grupoB));
		long tito = miembro(admin, "{\"nombres\":\"Tito\",\"apellidos\":\"Quispe\"}");

		mvc.perform(get("/api/grupos/{id}/candidatos", grupoA).param("q", "quispe").with(admin))
			.andExpect(jsonPath("$[*].nombreCompleto", contains("Rosa Quispe", "Tito Quispe")))
			.andExpect(jsonPath("$[0].grupo.nombre").value("Beto Beta"))
			.andExpect(jsonPath("$[1].grupo").doesNotExist());

		integrante(admin, grupoA, rosa).andExpect(status().isOk()).andExpect(jsonPath("$.nombreCompleto").value("Rosa Quispe"));
		integrante(admin, grupoA, tito).andExpect(status().isOk());
		mvc.perform(get("/api/grupos/{id}/miembros", grupoA).with(admin))
			.andExpect(jsonPath("$[*].id", contains((int) rosa, (int) tito)));
		mvc.perform(get("/api/grupos/{id}/miembros", grupoB).with(admin)).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/grupos/{id}/candidatos", grupoA).param("q", "quispe").with(admin))
			.andExpect(jsonPath("$.length()").value(0));

		mvc.perform(delete("/api/grupos/{id}/miembros/{miembroId}", grupoA, tito).with(admin))
			.andExpect(status().isNoContent());
		mvc.perform(delete("/api/grupos/{id}/miembros/{miembroId}", grupoB, rosa).with(admin))
			.andExpect(status().isNotFound());
		integrante(admin, grupoA, 999_999).andExpect(status().isNotFound());
		mvc.perform(get("/api/miembros/{id}", tito).with(admin)).andExpect(jsonPath("$.grupo").doesNotExist());
	}

	@Test
	void sinBusquedaSugiereLosCincoUltimosEnLlegarALaIglesia() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long grupo = PruebasNegocio.idDe(crear(admin,
				"{\"liderId\":%d}".formatted(miembro(admin, conIngreso("Lider", LocalDate.now().minusYears(5))))));
		for (int meses = 6; meses >= 1; meses--) {
			miembro(admin, conIngreso("Hace" + meses, LocalDate.now().minusMonths(meses)));
		}
		miembro(admin, "{\"nombres\":\"Hoy\",\"apellidos\":\"Registro\"}");

		mvc.perform(get("/api/grupos/{id}/candidatos", grupo).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombreCompleto",
					contains("Hoy Registro", "Hace1 Prueba", "Hace2 Prueba", "Hace3 Prueba", "Hace4 Prueba")));
	}

	private static String conIngreso(String nombres, LocalDate ingreso) {
		return "{\"nombres\":\"%s\",\"apellidos\":\"Prueba\",\"fechaIngreso\":\"%s\"}".formatted(nombres, ingreso);
	}

	@Test
	void noSeAgreganIntegrantesAUnGrupoInactivoNiMiembrosInactivos() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long grupo = PruebasNegocio.idDe(crear(admin,
				"{\"liderId\":%d}".formatted(miembro(admin, "{\"nombres\":\"G\",\"apellidos\":\"L\"}"))));
		long inactivo = miembro(admin, "{\"nombres\":\"In\",\"apellidos\":\"Activo\"}");
		mvc.perform(patch("/api/miembros/{id}/estado", inactivo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isOk());

		integrante(admin, grupo, inactivo).andExpect(status().isConflict());
		mvc.perform(patch("/api/grupos/{id}/estado", grupo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isOk());
		integrante(admin, grupo, miembro(admin, "{\"nombres\":\"Nu\",\"apellidos\":\"Evo\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void noSeDesactivaUnGrupoConMiembrosActivos() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long grupo = PruebasNegocio.idDe(crear(admin,
				"{\"liderId\":%d}".formatted(miembro(admin, "{\"nombres\":\"Be\",\"apellidos\":\"Tania\"}"))));
		miembro(admin, "{\"nombres\":\"Rosa\",\"apellidos\":\"Grupo\",\"grupoId\":%d}".formatted(grupo));

		mvc.perform(patch("/api/grupos/{id}/estado", grupo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isConflict());
	}

	@Test
	void unGrupoInactivoNoSeAsignaYNoSeDesactivaASuLider() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long lider = miembro(admin, "{\"nombres\":\"Lider\",\"apellidos\":\"Grupo\"}");
		long grupo = PruebasNegocio.idDe(crear(admin, "{\"liderId\":%d}".formatted(lider)));

		mvc.perform(patch("/api/miembros/{id}/estado", lider).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isConflict());
		mvc.perform(patch("/api/grupos/{id}/estado", grupo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isOk());
		mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"X\",\"apellidos\":\"Y\",\"grupoId\":%d}".formatted(grupo)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.grupoId").exists());
	}

	@Test
	void elNombreDeLosGruposTieneValorPorDefectoYSeConfigura() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));

		mvc.perform(get("/api/configuracion-iglesia").with(admin))
			.andExpect(jsonPath("$.nombreGrupoSingular").value("Grupo"))
			.andExpect(jsonPath("$.nombreGrupoPlural").value("Grupos"));
		mvc.perform(put("/api/configuracion-iglesia").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombreGrupoSingular\":\" Hogar de paz \",\"nombreGrupoPlural\":\"Hogares de paz\"}"))
			.andExpect(status().isOk());
		mvc.perform(get("/api/configuracion-iglesia").with(admin))
			.andExpect(jsonPath("$.nombreGrupoSingular").value("Hogar de paz"))
			.andExpect(jsonPath("$.nombreGrupoPlural").value("Hogares de paz"));
	}

	@Test
	void quienSoloVeMiembrosNoAdministraGrupos() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor lider = PruebasNegocio.liderDe(iglesia, 1L);

		mvc.perform(get("/api/grupos").with(lider)).andExpect(status().isOk());
		crear(lider, "{\"liderId\":1}").andExpect(status().isForbidden());
		mvc.perform(put("/api/grupos/{id}/miembros/{miembroId}", 1, 1).with(lider)).andExpect(status().isForbidden());
		mvc.perform(put("/api/configuracion-iglesia").with(lider)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombreGrupoSingular\":\"A\",\"nombreGrupoPlural\":\"B\"}")).andExpect(status().isForbidden());
	}

	private ResultActions crear(JwtRequestPostProcessor token, String cuerpo) throws Exception {
		return mvc.perform(post("/api/grupos").with(token).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private ResultActions integrante(JwtRequestPostProcessor token, long grupo, long miembro) throws Exception {
		return mvc.perform(put("/api/grupos/{id}/miembros/{miembroId}", grupo, miembro).with(token));
	}

	private long miembro(JwtRequestPostProcessor token, String cuerpo) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo)).andExpect(status().isCreated()));
	}

}
