package com.iglesia.api.aportes.controller;

import java.util.List;

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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registro, corrección, anulación, ámbito y totales de los aportes contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AporteIntegrationTest {

	private static final String RUTA = "/api/aportes";

	private static final String OCTUBRE = "desde=2026-10-01&hasta=2026-10-31";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void registraUnDiezmoSinRegistrarDatosPersonalesNiMontos(CapturedOutput salida) throws Exception {
		Iglesia iglesia = nueva();
		long juan = miembro(iglesia, "Juan");

		long id = PruebasNegocio.idDe(registrar(iglesia.admin, "2026-10-04", juan, iglesia.diezmo, "1234.56", "YAPE",
				"Por mi familia")
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.miembro.nombre").exists())
			.andExpect(jsonPath("$.categoria.nombre").value("Diezmo"))
			.andExpect(jsonPath("$.monto").value(1234.56)));

		assertThat(salida.getOut()).contains("Aporte registrado id=" + id)
			.doesNotContain("Juan")
			.doesNotContain("1234.56")
			.doesNotContain("Por mi familia");
	}

	@Test
	void validaAportanteFechaYMonto() throws Exception {
		Iglesia iglesia = nueva();

		registrar(iglesia.admin, "2026-10-04", null, iglesia.diezmo, "10", "EFECTIVO", null)
			.andExpect(status().isConflict()).andExpect(jsonPath("$.errores.miembroId").exists());
		registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "10", "EFECTIVO", null)
			.andExpect(status().isCreated()).andExpect(jsonPath("$.miembro").doesNotExist());
		registrar(iglesia.admin, "2999-01-01", null, iglesia.ofrenda, "10", "EFECTIVO", null)
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.traceId").exists());
		registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "0", "EFECTIVO", null)
			.andExpect(status().isBadRequest());
		registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "10.123", "EFECTIVO", null)
			.andExpect(status().isBadRequest());
	}

	@Test
	void corrigeYAnulaUnAporteQueLuegoNoSeModifica() throws Exception {
		Iglesia iglesia = nueva();
		long id = PruebasNegocio.idDe(registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "10", "EFECTIVO",
				null));

		mvc.perform(put(RUTA + "/{id}", id).with(iglesia.admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo("2026-10-05", null, iglesia.ofrenda, "15", "PLIN", null)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.monto").value(15))
			.andExpect(jsonPath("$.modificadoEn").exists());
		anular(iglesia.admin, id, "").andExpect(status().isBadRequest());
		anular(iglesia.admin, id, "Registrado dos veces").andExpect(status().isOk())
			.andExpect(jsonPath("$.anulado").value(true));
		anular(iglesia.admin, id, "Otra vez").andExpect(status().isConflict());
		mvc.perform(put(RUTA + "/{id}", id).with(iglesia.admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo("2026-10-05", null, iglesia.ofrenda, "15", "PLIN", null)))
			.andExpect(status().isConflict());
	}

	@Test
	void listaYTotalizaSinAnuladosYComparaConElPeriodoAnterior() throws Exception {
		Iglesia iglesia = nueva();
		long juan = miembro(iglesia, "Juan");
		registrar(iglesia.admin, "2026-10-04", juan, iglesia.diezmo, "100", "TRANSFERENCIA", null);
		registrar(iglesia.admin, "2026-10-11", null, iglesia.ofrenda, "30", "EFECTIVO", null);
		registrar(iglesia.admin, "2026-09-20", null, iglesia.ofrenda, "50", "YAPE", null);
		long anulado = PruebasNegocio.idDe(registrar(iglesia.admin, "2026-10-12", null, iglesia.ofrenda, "999",
				"EFECTIVO", null));
		anular(iglesia.admin, anulado, "Error");

		mvc.perform(get(RUTA + "?" + OCTUBRE).with(iglesia.admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].fecha", contains("2026-10-11", "2026-10-04")));
		mvc.perform(get(RUTA + "?" + OCTUBRE + "&incluirAnulados=true").with(iglesia.admin))
			.andExpect(jsonPath("$.content.length()").value(3));
		mvc.perform(get(RUTA + "/resumen?" + OCTUBRE).with(iglesia.admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.moneda").value("PEN"))
			.andExpect(jsonPath("$.total").value(130))
			.andExpect(jsonPath("$.cantidad").value(2))
			.andExpect(jsonPath("$.totalPeriodoAnterior").value(50))
			.andExpect(jsonPath("$.porCategoria[*].categoria.nombre", contains("Diezmo", "Ofrenda")))
			.andExpect(jsonPath("$.porMedio[*].medio", contains("EFECTIVO", "TRANSFERENCIA")));
	}

	@Test
	void rechazaRangosInvertidosOMayoresA366Dias() throws Exception {
		Iglesia iglesia = nueva();

		mvc.perform(get(RUTA + "?desde=2026-10-31&hasta=2026-10-01").with(iglesia.admin))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.hasta").exists());
		mvc.perform(get(RUTA + "/resumen?desde=2025-01-01&hasta=2026-10-01").with(iglesia.admin))
			.andExpect(status().isBadRequest());
		mvc.perform(get(RUTA).with(iglesia.admin)).andExpect(status().isBadRequest());
	}

	@Test
	void elLiderSoloVeSusPropiosAportesYNoRegistra() throws Exception {
		Iglesia iglesia = nueva();
		long lider = miembro(iglesia, "Lider");
		long otro = miembro(iglesia, "Otro");
		registrar(iglesia.admin, "2026-10-04", lider, iglesia.diezmo, "40", "YAPE", null);
		long ajeno = PruebasNegocio.idDe(registrar(iglesia.admin, "2026-10-04", otro, iglesia.diezmo, "60", "YAPE",
				null));
		registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "5", "EFECTIVO", null);
		JwtRequestPostProcessor token = PruebasNegocio.token(iglesia.id, List.of("LIDER"), lider,
				Permisos.APORTES_VER);

		mvc.perform(get(RUTA + "?" + OCTUBRE + "&miembroId=" + otro).with(token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].miembro.id").value(lider));
		mvc.perform(get(RUTA + "/resumen?" + OCTUBRE).with(token)).andExpect(jsonPath("$.total").value(40));
		mvc.perform(get(RUTA + "/{id}", ajeno).with(token)).andExpect(status().isNotFound());
		registrar(token, "2026-10-04", lider, iglesia.diezmo, "1", "YAPE", null).andExpect(status().isForbidden());
	}

	@Test
	void sinMiembroVinculadoNoSeVeNingunAporte() throws Exception {
		Iglesia iglesia = nueva();
		registrar(iglesia.admin, "2026-10-04", null, iglesia.ofrenda, "5", "EFECTIVO", null);
		JwtRequestPostProcessor token = PruebasNegocio.token(iglesia.id, List.of("LIDER"), null, Permisos.APORTES_VER);

		mvc.perform(get(RUTA + "?" + OCTUBRE).with(token)).andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(get(RUTA + "/resumen?" + OCTUBRE).with(token)).andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void noSeVenNiSeUsanAportesCategoriasNiMiembrosDeOtraIglesia() throws Exception {
		Iglesia otra = nueva();
		long ajeno = PruebasNegocio.idDe(registrar(otra.admin, "2026-10-04", null, otra.ofrenda, "5", "EFECTIVO",
				null));
		long miembroAjeno = miembro(otra, "Ajeno");
		Iglesia iglesia = nueva();

		mvc.perform(get(RUTA + "/{id}", ajeno).with(iglesia.admin)).andExpect(status().isNotFound());
		anular(iglesia.admin, ajeno, "x").andExpect(status().isNotFound());
		mvc.perform(get(RUTA + "?" + OCTUBRE).with(iglesia.admin)).andExpect(jsonPath("$.content.length()").value(0));
		registrar(iglesia.admin, "2026-10-04", null, otra.ofrenda, "5", "EFECTIVO", null)
			.andExpect(status().isConflict());
		registrar(iglesia.admin, "2026-10-04", miembroAjeno, iglesia.ofrenda, "5", "EFECTIVO", null)
			.andExpect(status().isConflict());
	}

	/** Iglesia de la prueba con su administrador y sus dos categorías. */
	private record Iglesia(long id, JwtRequestPostProcessor admin, long diezmo, long ofrenda) {
	}

	private Iglesia nueva() throws Exception {
		long id = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(id);
		return new Iglesia(id, admin, categoria(admin, "Diezmo", true), categoria(admin, "Ofrenda", false));
	}

	private long categoria(JwtRequestPostProcessor admin, String nombre, boolean exige) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/categorias-aporte").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"exigeAportante\":%s}".formatted(nombre, exige))));
	}

	private long miembro(Iglesia iglesia, String nombres) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(iglesia.admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\"}".formatted(nombres))));
	}

	private ResultActions registrar(JwtRequestPostProcessor token, String fecha, Long miembroId, long categoriaId,
			String monto, String medio, String peticion) throws Exception {
		return mvc.perform(post(RUTA).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cuerpo(fecha, miembroId, categoriaId, monto, medio, peticion)));
	}

	private ResultActions anular(JwtRequestPostProcessor token, long id, String motivo) throws Exception {
		return mvc.perform(patch(RUTA + "/{id}/anulacion", id).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"motivo\":\"%s\"}".formatted(motivo)));
	}

	private static String cuerpo(String fecha, Long miembroId, long categoriaId, String monto, String medio,
			String peticion) {
		return "{\"fecha\":\"%s\",\"miembroId\":%s,\"categoriaId\":%d,\"monto\":%s,\"medio\":\"%s\",\"peticion\":%s}"
			.formatted(fecha, miembroId, categoriaId, monto, medio, peticion == null ? "null" : "\"" + peticion + "\"");
	}

}
