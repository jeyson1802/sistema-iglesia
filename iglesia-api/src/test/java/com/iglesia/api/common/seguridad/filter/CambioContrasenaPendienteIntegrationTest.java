package com.iglesia.api.common.seguridad.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.error.component.FabricaProblemDetail;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.seguridad.exception.CambioContrasenaPendienteException;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con una contraseña temporal, todo endpoint protegido de la API responde 403 hasta que se cambie. */
@Import({ TestcontainersConfiguration.class, CambioContrasenaPendienteIntegrationTest.ControladorDePrueba.class })
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTracing
class CambioContrasenaPendienteIntegrationTest {

	private static final String RUTA_PRUEBA = "/api/prueba-cambio-contrasena";

	@Autowired
	private MockMvc mvc;

	@Test
	void conContrasenaTemporalRespondeAccesoDenegadoConProblemDetailYTraceId() throws Exception {
		ResultActions resultado = mvc
			.perform(get(RUTA_PRUEBA).with(jwt().jwt(token -> token.claim(ClaimsJwt.CAMBIO_CONTRASENA, true))))
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value(FabricaProblemDetail.TITULO_ACCESO_DENEGADO))
			.andExpect(jsonPath("$.detail").value(CambioContrasenaPendienteException.DETALLE));

		String traceId = resultado.andReturn().getResponse().getHeader(TrazaConstantes.CABECERA_TRACE_ID);
		assertThat(traceId).isNotBlank();
		resultado.andExpect(jsonPath("$." + TrazaConstantes.PROPIEDAD_TRACE_ID).value(traceId));
	}

	@Test
	void sinContrasenaTemporalRespondeNormalmente() throws Exception {
		mvc.perform(get(RUTA_PRUEBA).with(jwt().jwt(token -> token.claim(ClaimsJwt.CAMBIO_CONTRASENA, false))))
			.andExpect(status().isOk());
	}

	@Test
	void unTokenSinElClaimRespondeNormalmente() throws Exception {
		mvc.perform(get(RUTA_PRUEBA).with(jwt())).andExpect(status().isOk());
	}

	/** Endpoint protegido que solo existe en esta prueba. */
	@RestController
	static class ControladorDePrueba {

		@GetMapping(RUTA_PRUEBA)
		public void responder() {
			// Basta con que responda 200 si el filtro deja pasar la petición.
		}

	}

}
