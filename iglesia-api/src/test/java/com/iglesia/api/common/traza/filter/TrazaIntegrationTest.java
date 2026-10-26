package com.iglesia.api.common.traza.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Toda respuesta lleva X-Trace-Id y los logs de esa petición llevan el mismo traceId. */
@Import({ TestcontainersConfiguration.class, TrazaIntegrationTest.ConfiguracionLogDePrueba.class })
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTracing
@ExtendWith(OutputCaptureExtension.class)
class TrazaIntegrationTest {

	private static final String PATRON_TRACE_ID = "[0-9a-f]{32}";

	private static final String MENSAJE_DE_PRUEBA = "log-de-prueba-de-traza";

	@Autowired
	private MockMvc mvc;

	@Test
	void laSaludIncluyeLaCabeceraDeTraza() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(header().string(TrazaConstantes.CABECERA_TRACE_ID, matchesPattern(PATRON_TRACE_ID)));
	}

	@Test
	void unaPeticionSinTokenTambienIncluyeLaCabeceraDeTraza() throws Exception {
		mvc.perform(get("/api/miembros"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(TrazaConstantes.CABECERA_TRACE_ID, matchesPattern(PATRON_TRACE_ID)));
	}

	@Test
	void elLogDeLaPeticionLlevaElMismoTraceIdQueLaCabecera(CapturedOutput salida) throws Exception {
		String traceId = mvc.perform(get("/actuator/health"))
			.andReturn()
			.getResponse()
			.getHeader(TrazaConstantes.CABECERA_TRACE_ID);

		assertThat(salida.getOut().lines())
			.anySatisfy(linea -> assertThat(linea).contains(MENSAJE_DE_PRUEBA).contains(traceId));
	}

	@Test
	void corsExponeLaCabeceraDeTrazaAlFrontend() throws Exception {
		mvc.perform(get("/actuator/health").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
					containsString(TrazaConstantes.CABECERA_TRACE_ID)));
	}

	/** Emite un log dentro de la petición (después del filtro de traza) para comprobar la correlación. */
	@TestConfiguration(proxyBeanMethods = false)
	static class ConfiguracionLogDePrueba {

		private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionLogDePrueba.class);

		@Bean
		FilterRegistrationBean<OncePerRequestFilter> filtroQueRegistraUnLog() {
			FilterRegistrationBean<OncePerRequestFilter> registro = new FilterRegistrationBean<>(
					new OncePerRequestFilter() {
						@Override
						protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
								FilterChain cadena) throws ServletException, IOException {
							LOG.info(MENSAJE_DE_PRUEBA);
							cadena.doFilter(peticion, respuesta);
						}
					});
			registro.setOrder(FiltroCabeceraTraza.ORDEN + 1);
			return registro;
		}

	}

}
