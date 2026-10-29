package com.iglesia.api.iglesia.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * «Mi iglesia» contra PostgreSQL real: la iglesia sale del token, cualquier usuario la lee, solo quien tiene
 * {@code iglesia.personalizar} la edita y una iglesia no ve el logo de otra.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MiIglesiaIntegrationTest {

	private static final String RUTA = "/api/mi-iglesia";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cualquierUsuarioDeLaIglesiaLeeSuIdentidad() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);

		mvc.perform(get(RUTA).with(miembroDe(iglesia)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(iglesia))
			.andExpect(jsonPath("$.apariencia.colorPrimario").value("emerald"));
		mvc.perform(get(RUTA).with(jwt().jwt(token -> token.subject(PruebasNegocio.USUARIO))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void elAdministradorDeLaIglesiaPersonalizaSoloLaSuya(CapturedOutput salida) throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		String nombre = "Mi Iglesia " + iglesia;

		mvc.perform(put(RUTA).with(personalizadorDe(iglesia))
			.contentType(MediaType.APPLICATION_JSON)
			.content(IglesiaIntegrationTest.cuerpo(nombre, "150132", "\"mi@iglesia.org\"")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(iglesia))
			.andExpect(jsonPath("$.nombreComercial").value(nombre));
		mvc.perform(put(RUTA + "/apariencia").with(personalizadorDe(iglesia))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"preset\":\"Nora\",\"colorPrimario\":\"rose\",\"tipografia\":\"Poppins\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.preset").value("Nora"));

		mvc.perform(put(RUTA).with(miembroDe(iglesia))
			.contentType(MediaType.APPLICATION_JSON)
			.content(IglesiaIntegrationTest.cuerpo("Intruso", "150132", null)))
			.andExpect(status().isForbidden());
		assertThat(salida.getAll()).contains("Iglesia actualizada id=" + iglesia).doesNotContain("mi@iglesia.org");
	}

	@Test
	void unaIglesiaNoVeElLogoDeOtra() throws Exception {
		long una = PruebasNegocio.nuevaIglesia(jdbc);
		long otra = PruebasNegocio.nuevaIglesia(jdbc);

		mvc.perform(put(RUTA + "/logo").with(personalizadorDe(una))
			.contentType(MediaType.IMAGE_PNG)
			.content(ImagenesPrueba.PNG)).andExpect(status().isNoContent());

		mvc.perform(get(RUTA + "/logo").with(miembroDe(una))).andExpect(status().isOk());
		mvc.perform(get(RUTA + "/logo").with(miembroDe(otra))).andExpect(status().isNotFound());
	}

	private static JwtRequestPostProcessor miembroDe(long iglesiaId) {
		return jwt().jwt(token -> token.subject(PruebasNegocio.USUARIO).claim(ClaimsJwt.IGLESIA_ID, iglesiaId));
	}

	private static JwtRequestPostProcessor personalizadorDe(long iglesiaId) {
		return miembroDe(iglesiaId).authorities(new SimpleGrantedAuthority(Permisos.IGLESIA_PERSONALIZAR));
	}

}
