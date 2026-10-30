package com.iglesia.api.consolidacion.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ciclo completo de una ronda, ámbito del consolidador, reglas de integridad y aislamiento entre iglesias. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConsolidacionIntegrationTest {

	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unaRondaSeArmaSeReparteSeTrabajaYSeCierra() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long coordinadora = miembro(admin, "Coordinadora", null);
		long luis = miembro(admin, "Luis", null);
		long rosa = miembro(admin, "Rosa", null);
		List<Long> nuevos = List.of(miembro(admin, "Ana", null), miembro(admin, "Beto", null),
				miembro(admin, "Carla", null), miembro(admin, "Dario", null));
		JwtRequestPostProcessor coordinador = PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), coordinadora,
				Permisos.CONSOLIDACION_ADMINISTRAR);
		long resultado = PruebasNegocio.idDe(json(post("/api/consolidacion/resultados"), coordinador,
				"{\"nombre\":\"Volverá a asistir\"}"));
		json(post("/api/consolidacion/equipo"), coordinador, "{\"miembroId\":%d}".formatted(luis))
			.andExpect(status().isCreated());
		json(post("/api/consolidacion/equipo"), coordinador, "{\"miembroId\":%d}".formatted(rosa))
			.andExpect(status().isCreated());
		json(post("/api/consolidacion/equipo"), coordinador, "{\"miembroId\":%d}".formatted(luis))
			.andExpect(status().isConflict());
		long ronda = PruebasNegocio.idDe(json(post("/api/consolidacion/rondas"), coordinador,
				"{\"nombre\":\"Noviembre\",\"fechaInicio\":\"%s\"}".formatted(HOY)));
		String lote = "{\"miembroIds\":%s}".formatted(nuevos);

		json(post("/api/consolidacion/rondas/{id}/lote/vista-previa", ronda), coordinador, lote)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entrarian").value(4))
			.andExpect(jsonPath("$.personas.length()").value(4));
		json(post("/api/consolidacion/rondas/{id}/lote", ronda), coordinador, lote)
			.andExpect(jsonPath("$.agregadas").value(4));
		json(post("/api/consolidacion/rondas/{id}/lote", ronda), coordinador, lote)
			.andExpect(jsonPath("$.agregadas").value(0));
		json(post("/api/consolidacion/rondas/{id}/repartir", ronda), coordinador, "{\"soloSinAsignar\":true}")
			.andExpect(jsonPath("$.sinAsignar").value(0));
		mvc.perform(post("/api/consolidacion/rondas/{id}/iniciar", ronda).with(coordinador))
			.andExpect(jsonPath("$.estado").value("EN_CURSO"));

		JwtRequestPostProcessor consolidadorLuis = PruebasNegocio.token(iglesia, List.of("LIDER"), luis,
				Permisos.CONSOLIDACION_CONTACTAR);
		String misCasos = mvc
			.perform(get("/api/consolidacion/mis-casos").param("seccion", "SIN_CONTACTAR").with(consolidadorLuis))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long propio = ((Number) JsonPath.read(misCasos, "$.content[0].id")).longValue();
		long ajeno = idDeCasoDe(coordinador, ronda, rosa);

		mvc.perform(get("/api/consolidacion/casos/{id}", ajeno).with(consolidadorLuis)).andExpect(status().isNotFound());
		json(post("/api/consolidacion/casos/{id}/contactos", propio), consolidadorLuis,
				contacto("LLAMADA", "NO_ESTABA")).andExpect(status().isBadRequest());
		json(post("/api/consolidacion/casos/{id}/contactos", propio), consolidadorLuis, contacto("LLAMADA", "SE_HABLO"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.editable").value(true));
		mvc.perform(get("/api/consolidacion/casos/{id}", propio).with(consolidadorLuis))
			.andExpect(jsonPath("$.estado").value("EN_CONTACTO"))
			.andExpect(jsonPath("$.antes.iteracion").value(1))
			.andExpect(jsonPath("$.puedeActuar").value(true));
		json(post("/api/consolidacion/casos/{id}/cerrar", propio), consolidadorLuis,
				"{\"resultadoId\":%d}".formatted(resultado)).andExpect(jsonPath("$.estado").value("CERRADO"));

		json(post("/api/consolidacion/casos/{id}/contactos", ajeno), coordinador, contacto("VISITA", "NO_ESTABA"))
			.andExpect(status().isCreated());
		mvc.perform(post("/api/consolidacion/rondas/{id}/cerrar", ronda).with(coordinador))
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.noConcluidos").value(3));
		mvc.perform(get("/api/consolidacion/rondas/{id}/avance", ronda).with(coordinador))
			.andExpect(jsonPath("$.personas").value(4))
			.andExpect(jsonPath("$.contactadas").value(1))
			.andExpect(jsonPath("$.porResultado.length()").value(2));
		json(post("/api/consolidacion/casos/{id}/contactos", ajeno), coordinador, contacto("LLAMADA", "SE_HABLO"))
			.andExpect(status().isConflict());
	}

	@Test
	void unaPersonaNoTieneDosSeguimientosAbiertosYElConsolidadorSoloAbreASuRed() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long lider = miembro(admin, "Marta", null);
		long discipulo = miembro(admin, "Beto", lider);
		long otro = miembro(admin, "Carla", null);
		JwtRequestPostProcessor marta = PruebasNegocio.token(iglesia, List.of("LIDER"), lider,
				Permisos.CONSOLIDACION_CONTACTAR);
		JwtRequestPostProcessor coordinador = PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), null,
				Permisos.CONSOLIDACION_ADMINISTRAR);

		json(post("/api/consolidacion/casos"), marta, "{\"miembroId\":%d}".formatted(otro))
			.andExpect(status().isNotFound());
		json(post("/api/consolidacion/casos"), marta, "{\"miembroId\":%d}".formatted(discipulo))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.consolidador.id").value(lider));
		json(post("/api/consolidacion/casos"), coordinador,
				"{\"miembroId\":%d,\"consolidadorId\":%d}".formatted(discipulo, lider))
			.andExpect(status().isConflict());
		mvc.perform(get("/api/miembros/{id}/consolidacion", discipulo).with(marta))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.iteraciones").value(1))
			.andExpect(jsonPath("$.casoAbierto.consolidador.id").value(lider))
			.andExpect(jsonPath("$.puedeIniciar").value(false));
		mvc.perform(get("/api/miembros/{id}/consolidacion", otro).with(marta)).andExpect(status().isNotFound());
	}

	@Test
	void otraIglesiaNoVeLasRondasYUnaRondaEnCursoNoSeBorra() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana", null);
		JwtRequestPostProcessor coordinador = PruebasNegocio.token(iglesia, List.of("ADMIN_IGLESIA"), ana,
				Permisos.CONSOLIDACION_ADMINISTRAR);
		long ronda = PruebasNegocio.idDe(json(post("/api/consolidacion/rondas"), coordinador,
				"{\"nombre\":\"Diciembre\",\"fechaInicio\":\"%s\"}".formatted(HOY)));
		mvc.perform(post("/api/consolidacion/rondas/{id}/iniciar", ronda).with(coordinador))
			.andExpect(status().isConflict());
		json(post("/api/consolidacion/rondas/{id}/lote", ronda), coordinador, "{\"miembroIds\":[%d]}".formatted(ana));
		mvc.perform(post("/api/consolidacion/rondas/{id}/iniciar", ronda).with(coordinador))
			.andExpect(status().isOk());
		mvc.perform(delete("/api/consolidacion/rondas/{id}", ronda).with(coordinador))
			.andExpect(status().isConflict());

		JwtRequestPostProcessor otraIglesia = PruebasNegocio.token(PruebasNegocio.nuevaIglesia(jdbc),
				List.of("ADMIN_IGLESIA"), null, Permisos.CONSOLIDACION_ADMINISTRAR);
		mvc.perform(get("/api/consolidacion/rondas/{id}", ronda).with(otraIglesia)).andExpect(status().isNotFound());
		mvc.perform(get("/api/consolidacion/rondas").with(otraIglesia))
			.andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(get("/api/consolidacion/rondas").with(PruebasNegocio.liderDe(iglesia, ana)))
			.andExpect(status().isForbidden());
	}

	@Test
	void unaIglesiaNuevaRecibeLosResultadosIniciales() throws Exception {
		JwtRequestPostProcessor sistema = jwt().jwt(token -> token.subject("1"))
			.authorities(new SimpleGrantedAuthority(Permisos.IGLESIAS_ADMINISTRAR));
		long iglesia = PruebasNegocio.idDe(mvc.perform(post("/api/iglesias").with(sistema)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"nombreComercial":"Consolidación %s","denominacion":"Iglesia de prueba","direccion":"Av. Siempre Viva 1",\
				"distritoCodigo":"150132"}""".formatted(System.nanoTime()))).andExpect(status().isCreated()));

		mvc.perform(get("/api/consolidacion/resultados").with(PruebasNegocio.token(iglesia, List.of("LIDER"), null,
				Permisos.CONSOLIDACION_CONTACTAR)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(6))
			.andExpect(jsonPath("$[0].nombre").value("Se integró a un grupo"))
			.andExpect(jsonPath("$[5].orden").value(6));
	}

	private long idDeCasoDe(JwtRequestPostProcessor coordinador, long ronda, long consolidador) throws Exception {
		String casos = mvc
			.perform(get("/api/consolidacion/rondas/{id}/casos", ronda).param("consolidadorId", String.valueOf(consolidador))
				.with(coordinador))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(casos, "$.content[0].id")).longValue();
	}

	private static String contacto(String medio, String resultado) {
		return "{\"fechaHora\":\"%s\",\"medio\":\"%s\",\"resultado\":\"%s\"}".formatted(Instant.now(), medio, resultado);
	}

	private ResultActions json(MockHttpServletRequestBuilder peticion,
			JwtRequestPostProcessor token, String cuerpo) throws Exception {
		return mvc.perform(peticion.with(token).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private long miembro(JwtRequestPostProcessor admin, String nombres, Long lider) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\",\"fechaIngreso\":\"%s\",\"liderId\":%s}"
				.formatted(nombres, HOY.minusMonths(1), lider))));
	}

}
