package com.iglesia.api.miembros.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registro de miembros contra PostgreSQL real: ficha, jerarquía, ámbito del líder y aislamiento entre iglesias. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MiembroIntegrationTest {

	private static final String RUTA = "/api/miembros";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	// ---- Ficha

	@Test
	void registraConSoloNombresYApellidosYNoRegistraDatosPersonales(CapturedOutput salida) throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();

		long id = PruebasNegocio.idDe(guardar(admin, null, "{\"nombres\":\"  Juan \",\"apellidos\":\"Pérez\"}")
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, containsString(RUTA + "/")))
			.andExpect(jsonPath("$.nombres").value("Juan"))
			.andExpect(jsonPath("$.edad").doesNotExist())
			.andExpect(jsonPath("$.activo").value(true)));

		assertThat(salida.getOut()).contains("Miembro creado id=" + id).doesNotContain("Juan").doesNotContain("Pérez");
	}

	@Test
	void sinNombresOApellidosEsUnErrorDeDatosConTraceId() throws Exception {
		guardar(adminNuevo(), null, "{\"nombres\":\"Juan\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.apellidos").exists())
			.andExpect(jsonPath("$.traceId").isNotEmpty())
			.andExpect(header().exists(TrazaConstantes.CABECERA_TRACE_ID));
	}

	@Test
	void calculaLaEdadExactaOAproximadaYAceptaAmbasIndependientes() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		String nacimiento = LocalDate.now().minusYears(40).minusDays(1).toString();

		guardar(admin, null, ficha("Con", "Fecha", "\"fechaNacimiento\":\"" + nacimiento + "\""))
			.andExpect(jsonPath("$.edad").value(40))
			.andExpect(jsonPath("$.edadEsAproximada").value(false));
		guardar(admin, null, ficha("Con", "Edad", "\"edadAproximada\":30")).andExpect(jsonPath("$.edad").value(30))
			.andExpect(jsonPath("$.edadEsAproximada").value(true));
		guardar(admin, null, ficha("Las", "Dos", "\"edadAproximada\":30,\"fechaNacimiento\":\"" + nacimiento + "\""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.edad").value(40))
			.andExpect(jsonPath("$.edadEsAproximada").value(false))
			.andExpect(jsonPath("$.edadAproximada").value(30));
	}

	@Test
	void rechazaFechasFuturasTelefonosInvalidosYDistritosInexistentes() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		String futura = LocalDate.now().plusDays(2).toString();

		guardar(admin, null, ficha("A", "B", "\"fechaNacimiento\":\"" + futura + "\"")).andExpect(status().isBadRequest());
		guardar(admin, null, ficha("A", "B", "\"fechaIngreso\":\"" + futura + "\"")).andExpect(status().isBadRequest());
		guardar(admin, null, ficha("A", "B", "\"telefono\":\"98x\"")).andExpect(status().isBadRequest());
		guardar(admin, null, ficha("A", "B", "\"distritoCodigo\":\"999999\"")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errores.distritoCodigo").exists());
		guardar(admin, null, ficha("A", "B", "\"distritoCodigo\":\"150101\"")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.ubicacion.departamento.codigo").value("15"))
			.andExpect(jsonPath("$.ubicacion.provincia.codigo").value("1501"));
	}

	@Test
	void encuentraPosiblesDuplicadosSinDistinguirMayusculasNiEspacios() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long juan = alta(admin, "Juan", "Pérez", "");

		mvc.perform(get(RUTA + "/posibles-duplicados").param("nombres", "juan").param("apellidos", "PÉREZ ").with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) juan)));
		mvc.perform(get(RUTA + "/posibles-duplicados").param("nombres", "Juana").param("apellidos", "Pérez").with(admin))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void listaConBusquedaYFiltros() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long lider = alta(admin, "Ana", "Torres", "");
		alta(admin, "Luis", "Pérez", "\"condicion\":\"NUEVO\",\"liderId\":" + lider);
		alta(admin, "Rosa", "Pérez", "\"condicion\":\"CONSOLIDADO\"");

		mvc.perform(get(RUTA).param("q", "perez").with(admin))
			.andExpect(jsonPath("$.content[*].nombres", contains("Luis", "Rosa")));
		mvc.perform(get(RUTA).param("condicion", "NUEVO").with(admin))
			.andExpect(jsonPath("$.content[*].nombres", contains("Luis")))
			.andExpect(jsonPath("$.content[0].lider.nombre").value("Ana Torres"));
		mvc.perform(get(RUTA).param("liderId", String.valueOf(lider)).with(admin))
			.andExpect(jsonPath("$.page.totalElements").value(1));
		mvc.perform(get(RUTA).param("condicion", "ANTIGUO").with(admin)).andExpect(status().isBadRequest());
	}

	@Test
	void filtraPorDepartamentoProvinciaODistritoYDevuelveLaDireccion() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		alta(admin, "Lima", "Cercado", "\"distritoCodigo\":\"150101\",\"direccion\":\"Jr. Lampa 100\"");
		alta(admin, "Lima", "Miraflores", "\"distritoCodigo\":\"150122\"");
		alta(admin, "Callao", "Puerto", "\"distritoCodigo\":\"070101\"");
		alta(admin, "Sin", "Ubicacion", "");

		mvc.perform(get(RUTA).param("ubigeo", "15").with(admin))
			.andExpect(jsonPath("$.content[*].apellidos", contains("Cercado", "Miraflores")));
		mvc.perform(get(RUTA).param("ubigeo", "1501").with(admin)).andExpect(jsonPath("$.page.totalElements").value(2));
		mvc.perform(get(RUTA).param("ubigeo", "150122").with(admin))
			.andExpect(jsonPath("$.content[*].apellidos", contains("Miraflores")));
		mvc.perform(get(RUTA).param("ubigeo", "150101").with(admin))
			.andExpect(jsonPath("$.content[0].direccion").value("Jr. Lampa 100"))
			.andExpect(jsonPath("$.content[0].ubicacion.distrito.codigo").value("150101"))
			.andExpect(jsonPath("$.content[0].grupo").doesNotExist());
		mvc.perform(get(RUTA).param("ubigeo", "1").with(admin)).andExpect(status().isBadRequest());
		mvc.perform(get(RUTA).param("ubigeo", "15a1").with(admin)).andExpect(status().isBadRequest());
	}

	// ---- Jerarquía

	@Test
	void noSePermitenCiclosNiAutorreferencias() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long a = alta(admin, "A", "Ciclo", "");
		long b = alta(admin, "B", "Ciclo", "\"liderId\":" + a);
		long c = alta(admin, "C", "Ciclo", "\"liderId\":" + b);

		guardar(admin, a, ficha("A", "Ciclo", "\"liderId\":" + c)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.liderId").exists());
		guardar(admin, a, ficha("A", "Ciclo", "\"liderId\":" + a)).andExpect(status().isBadRequest());
		guardar(admin, a, ficha("A", "Ciclo", "\"invitadoPorId\":" + a)).andExpect(status().isBadRequest());
		guardar(admin, a, ficha("A", "Ciclo", "\"registradoPorId\":" + a)).andExpect(status().isBadRequest());
		mvc.perform(get(RUTA + "/{id}", a).with(admin)).andExpect(jsonPath("$.lider").doesNotExist());
	}

	@Test
	void devuelveLaCadenaDesdeElLiderDirectoHastaElPastor() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long pastor = alta(admin, "Pastor", "Cadena", "");
		designarPastor(admin, pastor).andExpect(status().isOk());
		long x = alta(admin, "X", "Cadena", "\"liderId\":" + pastor);
		long y = alta(admin, "Y", "Cadena", "\"liderId\":" + x);

		mvc.perform(get(RUTA + "/{id}/cadena", y).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) x, (int) pastor)))
			.andExpect(jsonPath("$[1].esPastorPrincipal").value(true));
		mvc.perform(get(RUTA + "/{id}", x).with(admin)).andExpect(jsonPath("$.cantidadLiderados").value(1));
	}

	@Test
	void devuelveLosLideradosDirectosConSuNivelYCuantosLideran() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long nivel = PruebasNegocio.idDe(mvc.perform(post("/api/niveles").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Líder\"}")));
		long raiz = alta(admin, "Raiz", "Arbol", "");
		long beto = alta(admin, "Beto", "Bravo", "\"liderId\":" + raiz + ",\"nivelId\":" + nivel);
		alta(admin, "Ana", "Alva", "\"liderId\":" + raiz);
		alta(admin, "Nieto", "Arbol", "\"liderId\":" + beto);
		long inactivo = alta(admin, "Inactivo", "Arbol", "\"liderId\":" + raiz);
		estado(admin, inactivo, false).andExpect(status().isOk());

		mvc.perform(get(RUTA + "/{id}/liderados", raiz).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombreCompleto", contains("Ana Alva", "Beto Bravo")))
			.andExpect(jsonPath("$[0].cantidadLiderados").value(0))
			.andExpect(jsonPath("$[1].nivel").value("Líder"))
			.andExpect(jsonPath("$[1].cantidadLiderados").value(1));
		mvc.perform(get(RUTA + "/{id}/liderados", 999_999).with(admin)).andExpect(status().isNotFound());
	}

	@Test
	void designarPastorDejaUnoSoloSinLiderYElPastorNoAceptaLider() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long anterior = alta(admin, "Anterior", "Pastor", "");
		long nuevo = alta(admin, "Nuevo", "Pastor", "\"liderId\":" + anterior);
		designarPastor(admin, anterior).andExpect(status().isOk());

		designarPastor(admin, nuevo).andExpect(status().isOk())
			.andExpect(jsonPath("$.esPastorPrincipal").value(true))
			.andExpect(jsonPath("$.lider").doesNotExist());
		mvc.perform(get(RUTA + "/{id}", anterior).with(admin)).andExpect(jsonPath("$.esPastorPrincipal").value(false));
		guardar(admin, nuevo, ficha("Nuevo", "Pastor", "\"liderId\":" + anterior)).andExpect(status().isConflict());
	}

	@Test
	void noSeDesactivaAlPastorNiAQuienLideraYSeReactiva() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long pastor = alta(admin, "P", "Estado", "");
		designarPastor(admin, pastor);
		long lider = alta(admin, "L", "Estado", "\"liderId\":" + pastor);
		long hoja = alta(admin, "H", "Estado", "\"liderId\":" + lider);

		estado(admin, pastor, false).andExpect(status().isConflict());
		estado(admin, lider, false).andExpect(status().isConflict());
		estado(admin, hoja, false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
		estado(admin, hoja, true).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
	}

	@Test
	void unaReferenciaInactivaNoSeAsignaPeroSeConservaSiYaEstaba() throws Exception {
		JwtRequestPostProcessor admin = adminNuevo();
		long invitador = alta(admin, "Inv", "Itador", "");
		long invitado = alta(admin, "Invi", "Tado", "\"invitadoPorId\":" + invitador);
		estado(admin, invitador, false).andExpect(status().isOk());

		guardar(admin, null, ficha("Otro", "Invitado", "\"invitadoPorId\":" + invitador))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errores.invitadoPorId").exists());
		guardar(admin, invitado, ficha("Invi", "Tado", "\"invitadoPorId\":" + invitador)).andExpect(status().isOk())
			.andExpect(jsonPath("$.invitadoPor.activo").value(false));
	}

	// ---- Ámbito del líder

	@Test
	void elLiderVeSuRedYSusSuperioresSoloEnLectura() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long pastor = alta(admin, "Pastor", "Red", "");
		designarPastor(admin, pastor);
		long lider = alta(admin, "Lider", "Red", "\"liderId\":" + pastor);
		long directo = alta(admin, "Directo", "Red", "\"liderId\":" + lider);
		long indirecto = alta(admin, "Indirecto", "Red", "\"liderId\":" + directo);
		long ajeno = alta(admin, "Ajeno", "Red", "\"liderId\":" + pastor);
		JwtRequestPostProcessor token = PruebasNegocio.liderDe(iglesia, lider);

		mvc.perform(get(RUTA).with(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id",
					containsInAnyOrder((int) pastor, (int) lider, (int) directo, (int) indirecto)));
		mvc.perform(get(RUTA + "/{id}", ajeno).with(token)).andExpect(status().isNotFound());
		mvc.perform(get(RUTA + "/{id}/cadena", indirecto).with(token)).andExpect(status().isOk());
		mvc.perform(get(RUTA + "/{id}/liderados", lider).with(token))
			.andExpect(jsonPath("$[*].id", contains((int) directo)));
		mvc.perform(get(RUTA + "/{id}/liderados", ajeno).with(token)).andExpect(status().isNotFound());
		mvc.perform(get(RUTA + "/opciones").with(token))
			.andExpect(jsonPath("$[*].id",
					containsInAnyOrder((int) pastor, (int) lider, (int) directo, (int) indirecto)));
		guardar(token, null, ficha("No", "Puede", "")).andExpect(status().isForbidden());
		guardar(token, directo, ficha("Directo", "Red", "")).andExpect(status().isForbidden());
	}

	@Test
	void unUsuarioSinAdministracionNiLiderazgoNoVeANadie() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		long miembro = alta(PruebasNegocio.administradorDe(iglesia), "Solo", "Miembro", "");
		JwtRequestPostProcessor secretaria = PruebasNegocio.token(iglesia, List.of("MIEMBRO"), miembro, "miembros.ver");

		mvc.perform(get(RUTA).with(secretaria)).andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(get(RUTA + "/{id}", miembro).with(secretaria)).andExpect(status().isNotFound());
	}

	@Test
	void elAdministradorDelSistemaDentroDeUnaIglesiaVeTodaLaIglesia() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		long miembro = alta(PruebasNegocio.administradorDe(iglesia), "Visible", "Sistema", "");
		JwtRequestPostProcessor sistema = PruebasNegocio.token(iglesia, List.of("ADMIN_SISTEMA"), null, "miembros.ver");

		mvc.perform(get(RUTA + "/{id}", miembro).with(sistema)).andExpect(status().isOk());
	}

	// ---- Aislamiento

	@Test
	void noSeVenNiSeAsignanMiembrosNivelesNiGruposDeOtraIglesia() throws Exception {
		JwtRequestPostProcessor otra = adminNuevo();
		long miembroAjeno = alta(otra, "Ajeno", "Otro", "");
		long nivelAjeno = PruebasNegocio.idDe(mvc.perform(post("/api/niveles").with(otra)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Ajeno\"}")));
		long grupoAjeno = PruebasNegocio.idDe(mvc.perform(post("/api/grupos").with(otra)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":" + miembroAjeno + "}")));
		JwtRequestPostProcessor admin = adminNuevo();

		mvc.perform(get(RUTA + "/{id}", miembroAjeno).with(admin)).andExpect(status().isNotFound());
		guardar(admin, miembroAjeno, ficha("X", "Y", "")).andExpect(status().isNotFound());
		guardar(admin, null, ficha("X", "Y", "\"liderId\":" + miembroAjeno)).andExpect(status().isConflict());
		guardar(admin, null, ficha("X", "Y", "\"nivelId\":" + nivelAjeno)).andExpect(status().isConflict());
		guardar(admin, null, ficha("X", "Y", "\"grupoId\":" + grupoAjeno)).andExpect(status().isConflict());
	}

	private JwtRequestPostProcessor adminNuevo() {
		return PruebasNegocio.administradorDe(PruebasNegocio.nuevaIglesia(jdbc));
	}

	private long alta(JwtRequestPostProcessor token, String nombres, String apellidos, String extra) throws Exception {
		return PruebasNegocio.idDe(guardar(token, null, ficha(nombres, apellidos, extra)).andExpect(status().isCreated()));
	}

	private ResultActions guardar(JwtRequestPostProcessor token, Long id, String cuerpo) throws Exception {
		var peticion = id == null ? post(RUTA) : put(RUTA + "/{id}", id);
		return mvc.perform(peticion.with(token).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private ResultActions designarPastor(JwtRequestPostProcessor token, long id) throws Exception {
		return mvc.perform(put(RUTA + "/{id}/pastor-principal", id).with(token));
	}

	private ResultActions estado(JwtRequestPostProcessor token, long id, boolean activo) throws Exception {
		return mvc.perform(patch(RUTA + "/{id}/estado", id).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"activo\":" + activo + "}"));
	}

	private static String ficha(String nombres, String apellidos, String extra) {
		String resto = extra.isEmpty() ? "" : "," + extra;
		return "{\"nombres\":\"%s\",\"apellidos\":\"%s\"%s}".formatted(nombres, apellidos, resto);
	}

}
