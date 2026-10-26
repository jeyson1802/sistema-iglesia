package com.iglesia.api.common.traza.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.ClaimsJwt;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los logs de una petición autenticada llevan usuarioId e iglesiaId, y la siguiente petición no los hereda. */
@Import({ TestcontainersConfiguration.class, ContextoUsuarioIntegrationTest.ControladorDePrueba.class })
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ContextoUsuarioIntegrationTest {

	private static final String RUTA_PRUEBA = "/api/prueba-contexto";

	private static final String MENSAJE_DE_PRUEBA = "log-de-prueba-de-contexto";

	private static final String USUARIO_ID = "4242";

	private static final long IGLESIA_ID = 77L;

	@Autowired
	private MockMvc mvc;

	@Test
	void elLogDeUnaPeticionAutenticadaLlevaElUsuarioYLaIglesia(CapturedOutput salida) throws Exception {
		mvc.perform(get(RUTA_PRUEBA).with(jwt().jwt(token -> token.subject(USUARIO_ID)
			.claim(ClaimsJwt.IGLESIA_ID, IGLESIA_ID)))).andExpect(status().isOk());

		assertThat(salida.getOut().lines()).filteredOn(linea -> linea.contains(MENSAJE_DE_PRUEBA))
			.singleElement()
			.satisfies(linea -> assertThat(linea)
				.contains(campoMdc(TrazaConstantes.MDC_USUARIO_ID, USUARIO_ID))
				.contains(campoMdc(TrazaConstantes.MDC_IGLESIA_ID, String.valueOf(IGLESIA_ID))));
	}

	@Test
	void unaPeticionPosteriorEnElMismoHiloNoHeredaElUsuario(CapturedOutput salida) throws Exception {
		mvc.perform(get(RUTA_PRUEBA).with(jwt().jwt(token -> token.subject(USUARIO_ID)
			.claim(ClaimsJwt.IGLESIA_ID, IGLESIA_ID)))).andExpect(status().isOk());

		// MockMvc ejecuta en el hilo de la prueba: la segunda petición reutiliza el mismo hilo y su MDC.
		mvc.perform(get(RUTA_PRUEBA)).andExpect(status().isUnauthorized());

		assertThat(salida.getOut().lines()).filteredOn(linea -> linea.contains("401 GET " + RUTA_PRUEBA))
			.singleElement()
			.satisfies(linea -> assertThat(linea).doesNotContain(TrazaConstantes.MDC_USUARIO_ID)
				.doesNotContain(TrazaConstantes.MDC_IGLESIA_ID));
	}

	/** Así aparece una entrada del MDC en el log estructurado ECS que se usa sin perfil. */
	private static String campoMdc(String clave, String valor) {
		return "\"" + clave + "\":\"" + valor + "\"";
	}

	/** Endpoint que solo existe en esta prueba y emite un log dentro de la petición. */
	@RestController
	static class ControladorDePrueba {

		private static final Logger LOG = LoggerFactory.getLogger(ControladorDePrueba.class);

		@GetMapping(RUTA_PRUEBA)
		public void registrar() {
			LOG.info(MENSAJE_DE_PRUEBA);
		}

	}

}
