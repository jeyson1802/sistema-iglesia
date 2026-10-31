package com.iglesia.api.perfil.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Portal del miembro: sus datos, su asistencia y su QR, tomados solo del token y sin permisos de módulo. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MiPerfilIntegrationTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unMiembroSinPermisosVeSuPropiaInformacion() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		DatosEstadisticas datos = new DatosEstadisticas(mvc, jdbc, iglesia);
		long luis = datos.miembro("Luis", null, HOY.minusYears(2));
		long ana = datos.miembro("Ana", luis, HOY.minusMonths(3));
		long grupo = datos.grupo(luis);
		datos.integrar(grupo, ana);
		datos.marcar(datos.culto(HOY), ana);
		JwtRequestPostProcessor miembro = PruebasNegocio.token(iglesia, List.of("MIEMBRO"), ana);

		String cuerpo = mvc.perform(get("/api/mi-perfil").with(miembro))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.miembroId").value(ana))
			.andExpect(jsonPath("$.nombres").value("Ana"))
			.andExpect(jsonPath("$.lider").value("Luis Prueba"))
			.andExpect(jsonPath("$.grupo.id").value(grupo))
			.andExpect(jsonPath("$.fechaIngreso").value(HOY.minusMonths(3).toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(cuerpo).doesNotContain("telefono").doesNotContain("aporte");
		mvc.perform(get("/api/mi-perfil/asistencia").with(miembro))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ultimas.length()").value(1));
		mvc.perform(get("/api/mi-perfil/codigo-asistencia").with(miembro))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contenidoQr", startsWith("ASIS:")));
	}

	@Test
	void sinMiembroVinculadoResponde404() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor sinVinculo = PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null);
		JwtRequestPostProcessor deOtraIglesia = PruebasNegocio.token(iglesia, List.of("MIEMBRO"), 999_999L);

		mvc.perform(get("/api/mi-perfil").with(sinVinculo))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Tu usuario no está vinculado a un miembro de la iglesia"))
			.andExpect(jsonPath("$.traceId", notNullValue()));
		mvc.perform(get("/api/mi-perfil/asistencia").with(deOtraIglesia)).andExpect(status().isNotFound());
		mvc.perform(get("/api/mi-perfil/codigo-asistencia").with(sinVinculo)).andExpect(status().isNotFound());
	}

}
