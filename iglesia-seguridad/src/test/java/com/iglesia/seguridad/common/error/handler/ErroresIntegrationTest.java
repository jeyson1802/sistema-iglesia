package com.iglesia.seguridad.common.error.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.common.error.component.FabricaProblemDetail;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Todo error responde ProblemDetail en español con el mismo traceId que la cabecera, y se registra con su nivel. */
@Import({ TestcontainersConfiguration.class, ErroresIntegrationTest.ControladorDePrueba.class })
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTracing
@ExtendWith(OutputCaptureExtension.class)
class ErroresIntegrationTest {

	private static final String RUTA_PRUEBA = "/api/prueba-errores";

	private static final String DETALLE_INTERNO = "detalle-interno-que-no-debe-salir";

	private static final String MENSAJE_NO_ENCONTRADO = "Recurso de prueba no encontrado";

	@Autowired
	private MockMvc mvc;

	@Test
	void sinTokenResponde401ConProblemDetailYTraceId() throws Exception {
		ResultActions resultado = mvc.perform(get("/api/usuarios/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
			.andExpect(jsonPath("$.title").value(FabricaProblemDetail.TITULO_NO_AUTENTICADO))
			.andExpect(jsonPath("$.detail").value(FabricaProblemDetail.DETALLE_NO_AUTENTICADO));

		esProblemDetailConTraceIdDeLaCabecera(resultado);
	}

	@Test
	void sinPermisoResponde403ConProblemDetailYTraceId() throws Exception {
		ResultActions resultado = mvc
			.perform(get(RUTA_PRUEBA + "/restringido").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MIEMBRO"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.title").value(FabricaProblemDetail.TITULO_ACCESO_DENEGADO));

		esProblemDetailConTraceIdDeLaCabecera(resultado);
	}

	@Test
	void recursoInexistenteResponde404YRegistraWarnSinStackTrace(CapturedOutput salida) throws Exception {
		ResultActions resultado = mvc.perform(get(RUTA_PRUEBA + "/no-encontrado").with(jwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value(MENSAJE_NO_ENCONTRADO));

		esProblemDetailConTraceIdDeLaCabecera(resultado);
		assertThat(salida.getOut().lines())
			.filteredOn(linea -> linea.contains("404 GET " + RUTA_PRUEBA + "/no-encontrado: " + MENSAJE_NO_ENCONTRADO))
			.singleElement()
			.satisfies(linea -> assertThat(linea).contains("WARN"));
		assertThat(salida.getOut()).doesNotContain(RecursoNoEncontradoException.class.getName());
	}

	@Test
	void errorInesperadoResponde500GenericoYRegistraElStackTraceUnaSolaVez(CapturedOutput salida) throws Exception {
		ResultActions resultado = mvc.perform(get(RUTA_PRUEBA + "/fallo").with(jwt()))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.title").value(FabricaProblemDetail.TITULO_ERROR_INTERNO))
			.andExpect(jsonPath("$.detail").value(FabricaProblemDetail.DETALLE_ERROR_INTERNO))
			.andExpect(content().string(not(containsString(DETALLE_INTERNO))));

		String traceId = esProblemDetailConTraceIdDeLaCabecera(resultado);
		assertThat(salida.getOut().lines()).filteredOn(linea -> linea.contains("Error no controlado"))
			.singleElement()
			.satisfies(linea -> assertThat(linea).contains("ERROR")
				.contains("GET " + RUTA_PRUEBA + "/fallo")
				.contains(traceId));
		assertThat(salida.getOut()).contains(IllegalStateException.class.getName() + ": " + DETALLE_INTERNO)
			.contains("at " + ControladorDePrueba.class.getName());
	}

	/** Comprueba el tipo de contenido y que el traceId del cuerpo sea el de la cabecera; lo devuelve. */
	private static String esProblemDetailConTraceIdDeLaCabecera(ResultActions resultado) throws Exception {
		String traceId = resultado.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andReturn()
			.getResponse()
			.getHeader(TrazaConstantes.CABECERA_TRACE_ID);
		assertThat(traceId).isNotBlank();
		resultado.andExpect(jsonPath("$." + TrazaConstantes.PROPIEDAD_TRACE_ID).value(traceId));
		return traceId;
	}

	/** Endpoints que solo existen en esta prueba para provocar cada tipo de error. */
	@RestController
	@RequestMapping(RUTA_PRUEBA)
	static class ControladorDePrueba {

		@GetMapping("/restringido")
		@PreAuthorize("hasRole('ADMIN_SISTEMA')")
		public void restringido() {
			// Nunca se ejecuta en la prueba: el usuario no tiene el rol.
		}

		@GetMapping("/no-encontrado")
		public void noEncontrado() {
			throw new RecursoNoEncontradoException(MENSAJE_NO_ENCONTRADO);
		}

		@GetMapping("/fallo")
		public void fallo() {
			throw new IllegalStateException(DETALLE_INTERNO);
		}

	}

}
