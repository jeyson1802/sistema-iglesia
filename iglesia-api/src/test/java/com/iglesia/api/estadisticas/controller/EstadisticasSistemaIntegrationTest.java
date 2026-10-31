package com.iglesia.api.estadisticas.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.estadisticas.DatosEstadisticas;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Estadísticas entre iglesias por las funciones SECURITY DEFINER: solo en la vista global, solo agregados y nunca
 * datos de personas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EstadisticasSistemaIntegrationTest {

	private static final String RUTA = "/api/estadisticas/sistema";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void enLaVistaGlobalCuentaTodasLasIglesiasSinDatosDePersonas() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		new DatosEstadisticas(mvc, jdbc, iglesia).miembro("Zacarías", null, LocalDate.now());

		String cuerpo = mvc.perform(get(RUTA).param("periodo", "PERSONALIZADO")
				.param("desde", LocalDate.now().withDayOfMonth(1).minusMonths(11).toString())
				.param("hasta", LocalDate.now().toString()).with(administradorDelSistema(null)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.iglesias", greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.miembros", greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.iglesiasNuevas", greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.iglesiasConActividad", greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.serie.length()").value(12))
			.andExpect(jsonPath("$.porTamano.length()").value(6))
			.andExpect(jsonPath("$.mayores.length()", greaterThanOrEqualTo(1)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(cuerpo).doesNotContain("Zacarías").doesNotContainIgnoringCase("monto").doesNotContain("aporte");
	}

	@Test
	void dentroDeUnaIglesiaOSinElPermisoResponde403() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);

		mvc.perform(get(RUTA).with(administradorDelSistema(iglesia))).andExpect(status().isForbidden());
		mvc.perform(get(RUTA).with(PruebasNegocio.administradorDe(iglesia))).andExpect(status().isForbidden());
		mvc.perform(get(RUTA).with(PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null,
				Permisos.ESTADISTICAS_SISTEMA)))
			.andExpect(status().isForbidden());
	}

	@Test
	void elRolDeLaAplicacionSoloPuedeEjecutarLasFuncionesNoLeerOtrasIglesias() {
		assertThat(jdbc.queryForObject("""
				select count(*) from information_schema.routine_privileges
				where routine_name like 'estadisticas_sistema_%' and privilege_type = 'EXECUTE'
				  and grantee = 'PUBLIC'""", Long.class)).isZero();
		assertThat(jdbc.queryForList("""
				select prosecdef from pg_proc where proname like 'estadisticas_sistema_%'""", Boolean.class))
			.hasSize(5)
			.containsOnly(true);
	}

	private static JwtRequestPostProcessor administradorDelSistema(Long iglesiaId) {
		return jwt().jwt(token -> {
			token.subject(PruebasNegocio.USUARIO).claim(ClaimsJwt.ROLES, List.of("ADMIN_SISTEMA"));
			if (iglesiaId != null) {
				token.claim(ClaimsJwt.IGLESIA_ID, iglesiaId);
			}
		}).authorities(new SimpleGrantedAuthority(Permisos.ESTADISTICAS_SISTEMA));
	}

}
