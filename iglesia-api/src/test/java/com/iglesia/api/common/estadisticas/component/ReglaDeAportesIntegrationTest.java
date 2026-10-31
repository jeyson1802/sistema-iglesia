package com.iglesia.api.common.estadisticas.component;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.iglesia.api.common.estadisticas.DatosEstadisticas;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regla de aportes del SPEC 11: aunque la iglesia tenga aportes y el usuario pueda verlos, ningún endpoint de
 * estadísticas ni del portal del miembro, salvo {@code /api/estadisticas/aportes}, devuelve datos de aportes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReglaDeAportesIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@ParameterizedTest
	@ValueSource(strings = { "/api/estadisticas/miembros", "/api/estadisticas/grupos", "/api/estadisticas/asistencia",
			"/api/estadisticas/consolidacion", "/api/mi-perfil", "/api/mi-perfil/asistencia" })
	void ningunaOtraRespuestaIncluyeAportes(String ruta) throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = new DatosEstadisticas(mvc, jdbc, iglesia).miembro("Ana", null, LocalDate.now());
		long categoria = PruebasNegocio.idDe(mvc.perform(post("/api/categorias-aporte").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Diezmo de prueba\",\"exigeAportante\":false}")));
		mvc.perform(post("/api/aportes").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fecha\":\"%s\",\"miembroId\":%d,\"categoriaId\":%d,\"monto\":99.90,\"medio\":\"YAPE\"}"
				.formatted(LocalDate.now(), ana, categoria)))
			.andExpect(status().isCreated());
		JwtRequestPostProcessor todo = PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), ana,
				Permisos.MIEMBROS_VER, Permisos.GRUPOS_VER, Permisos.ASISTENCIA_VER, Permisos.CONSOLIDACION_VER,
				Permisos.APORTES_VER);

		String cuerpo = mvc.perform(get(ruta).with(todo))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(cuerpo).doesNotContainIgnoringCase("aporte")
			.doesNotContainIgnoringCase("monto")
			.doesNotContain("99.9")
			.doesNotContainIgnoringCase("diezmo");
	}

}
