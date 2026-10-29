package com.iglesia.api.grupos.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Grupos y nombre configurable de los grupos contra PostgreSQL real. */
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
				{"nombre":"Los Olivos","liderId":%d,"diaReunion":"JUEVES","horaReunion":"19:30","distritoCodigo":"150135"}
				""".formatted(lider)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.lider.nombre").value("Ana Lider"))
			.andExpect(jsonPath("$.ubicacion.distrito.codigo").value("150135"))
			.andExpect(jsonPath("$.cantidadMiembros").value(0)));
		miembro(admin, "{\"nombres\":\"Luis\",\"apellidos\":\"Grupo\",\"grupoId\":%d}".formatted(grupo));

		mvc.perform(get("/api/grupos/{id}", grupo).with(admin)).andExpect(jsonPath("$.cantidadMiembros").value(1));
	}

	@Test
	void noSeDesactivaUnGrupoConMiembrosActivosNiSeRepiteElNombre() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long grupo = PruebasNegocio.idDe(crear(admin, "{\"nombre\":\"Betania\"}"));
		miembro(admin, "{\"nombres\":\"Rosa\",\"apellidos\":\"Grupo\",\"grupoId\":%d}".formatted(grupo));

		mvc.perform(patch("/api/grupos/{id}/estado", grupo).with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":false}")).andExpect(status().isConflict());
		crear(admin, "{\"nombre\":\"BETANIA\"}").andExpect(status().isConflict());
	}

	@Test
	void unGrupoInactivoNoSeAsignaYNoSeDesactivaASuLider() throws Exception {
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
		long lider = miembro(admin, "{\"nombres\":\"Lider\",\"apellidos\":\"Grupo\"}");
		long grupo = PruebasNegocio.idDe(crear(admin, "{\"nombre\":\"Sion\",\"liderId\":%d}".formatted(lider)));

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
		crear(lider, "{\"nombre\":\"No\"}").andExpect(status().isForbidden());
		mvc.perform(put("/api/configuracion-iglesia").with(lider)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombreGrupoSingular\":\"A\",\"nombreGrupoPlural\":\"B\"}")).andExpect(status().isForbidden());
	}

	private ResultActions crear(JwtRequestPostProcessor token, String cuerpo) throws Exception {
		return mvc.perform(post("/api/grupos").with(token).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private long miembro(JwtRequestPostProcessor token, String cuerpo) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo)).andExpect(status().isCreated()));
	}

}
