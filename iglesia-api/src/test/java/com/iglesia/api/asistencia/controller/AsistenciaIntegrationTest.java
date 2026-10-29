package com.iglesia.api.asistencia.controller;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Horarios, apertura de reuniones, marcas con QR y a mano, aislamiento entre iglesias y ámbito del líder. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AsistenciaIntegrationTest {

	private static final String HOY = LocalDate.now().toString();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void marcaConElQrUnaSolaVezYNoRegistraNombresNiCodigos(CapturedOutput salida) throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long reunion = abrirCulto(admin, culto(admin, "Culto dominical"));
		long ana = miembro(admin, "Ana");
		String qr = contenidoQr(admin, ana);

		marcarCodigo(admin, reunion, qr).andExpect(status().isCreated())
			.andExpect(jsonPath("$.yaRegistrado").value(false))
			.andExpect(jsonPath("$.totalAsistentes").value(1));
		marcarCodigo(admin, reunion, qr.toLowerCase().replace("asis:", "")).andExpect(status().isOk())
			.andExpect(jsonPath("$.yaRegistrado").value(true))
			.andExpect(jsonPath("$.totalAsistentes").value(1));
		mvc.perform(get("/api/reuniones/{id}/asistentes", reunion).with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].miembroId").value(ana))
			.andExpect(jsonPath("$.content[0].medio").value("QR"));

		assertThat(salida.getOut()).contains("Asistencia marcada reunionId=" + reunion + " miembroId=" + ana)
			.doesNotContain("Ana Prueba")
			.doesNotContain(qr.substring("ASIS:".length()));
	}

	@Test
	void abrirElMismoCultoDosVecesDevuelveLaMismaReunion() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long culto = culto(admin, "Oración");

		long primera = PruebasNegocio.idDe(abrir(admin, culto).andExpect(status().isCreated()));
		long segunda = PruebasNegocio.idDe(abrir(admin, culto).andExpect(status().isOk()));

		assertThat(segunda).isEqualTo(primera);
		mvc.perform(post("/api/reuniones").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"cultoProgramadoId\":%d,\"fecha\":\"2999-01-01\"}".formatted(culto)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.traceId").exists())
			.andExpect(header().exists("X-Trace-Id"));
	}

	@Test
	void unQrAjenoOUnCodigoDeOtraIglesiaNoMarcan() throws Exception {
		long iglesiaA = PruebasNegocio.nuevaIglesia(jdbc);
		long iglesiaB = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor adminA = PruebasNegocio.administradorDe(iglesiaA);
		JwtRequestPostProcessor adminB = PruebasNegocio.administradorDe(iglesiaB);
		long reunionA = abrirCulto(adminA, culto(adminA, "Dominical"));
		String qrDeB = contenidoQr(adminB, miembro(adminB, "Beto"));

		marcarCodigo(adminA, reunionA, "https://pago.example/qr").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Este QR no es un código de asistencia"));
		marcarCodigo(adminA, reunionA, qrDeB).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Código no encontrado en esta iglesia"));
		mvc.perform(get("/api/reuniones/{id}", reunionA).with(adminB)).andExpect(status().isNotFound());
	}

	@Test
	void elLiderSoloTomaAsistenciaEnSuGrupo() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana");
		long luis = miembro(admin, "Luis");
		long grupoAna = grupo(admin, ana);
		long grupoLuis = grupo(admin, luis);
		long beto = miembro(admin, "Beto");
		mvc.perform(put("/api/grupos/{id}/miembros/{miembroId}", grupoAna, beto).with(admin))
			.andExpect(status().is2xxSuccessful());
		JwtRequestPostProcessor liderAna = PruebasNegocio.token(iglesia, List.of("LIDER"), ana, Permisos.MIEMBROS_VER,
				Permisos.ASISTENCIA_GRUPO);

		long reunion = PruebasNegocio.idDe(abrirGrupo(liderAna, grupoAna).andExpect(status().isCreated()));
		abrirGrupo(liderAna, grupoLuis).andExpect(status().isNotFound());
		abrir(liderAna, culto(admin, "Dominical")).andExpect(status().isNotFound());

		mvc.perform(get("/api/reuniones/{id}/integrantes", reunion).with(liderAna))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.miembroId == %d)].presente".formatted(beto)).value(false));
		marcarMiembro(liderAna, reunion, beto).andExpect(status().isCreated());
		mvc.perform(delete("/api/reuniones/{id}/asistencias/{miembroId}", reunion, beto).with(liderAna))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/reuniones").with(liderAna))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void losFotochecksEnLoteUsanLosFiltrosDelListadoYGeneranLosCodigos() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor admin = PruebasNegocio.administradorDe(iglesia);
		long ana = miembro(admin, "Ana");
		miembro(admin, "Luis");

		mvc.perform(get("/api/miembros/fotochecks").param("q", "ana").with(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].miembroId").value(ana))
			.andExpect(jsonPath("$[0].contenidoQr").value(org.hamcrest.Matchers.startsWith("ASIS:")));
	}

	@Test
	void sinPermisosDeAsistenciaNoSeEntra() throws Exception {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		JwtRequestPostProcessor soloMiembros = PruebasNegocio.token(iglesia, List.of("LIDER"), null,
				Permisos.MIEMBROS_VER);

		mvc.perform(get("/api/reuniones/hoy").with(soloMiembros)).andExpect(status().isForbidden());
		mvc.perform(get("/api/cultos-programados").with(soloMiembros)).andExpect(status().isForbidden());
		mvc.perform(post("/api/reuniones/eventos").with(soloMiembros)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"Vigilia\",\"fechaInicio\":\"%s\"}".formatted(HOY)))
			.andExpect(status().isForbidden());
	}

	private long culto(JwtRequestPostProcessor admin, String nombre) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/cultos-programados").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombre\":\"%s\",\"diaSemana\":\"DOMINGO\",\"hora\":\"10:00\"}".formatted(nombre))));
	}

	private ResultActions abrir(JwtRequestPostProcessor token, long culto) throws Exception {
		return mvc.perform(post("/api/reuniones").with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"cultoProgramadoId\":%d,\"fecha\":\"%s\"}".formatted(culto, HOY)));
	}

	private ResultActions abrirGrupo(JwtRequestPostProcessor token, long grupo) throws Exception {
		return mvc.perform(post("/api/reuniones").with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"grupoId\":%d,\"fecha\":\"%s\"}".formatted(grupo, HOY)));
	}

	private long abrirCulto(JwtRequestPostProcessor admin, long culto) throws Exception {
		return PruebasNegocio.idDe(abrir(admin, culto));
	}

	private long miembro(JwtRequestPostProcessor admin, String nombres) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\"}".formatted(nombres))));
	}

	private long grupo(JwtRequestPostProcessor admin, long lider) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/grupos").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":%d}".formatted(lider))));
	}

	private String contenidoQr(JwtRequestPostProcessor admin, long miembro) throws Exception {
		String cuerpo = mvc.perform(get("/api/miembros/{id}/codigo-asistencia", miembro).with(admin))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(cuerpo, "$.contenidoQr");
	}

	private ResultActions marcarCodigo(JwtRequestPostProcessor token, long reunion, String codigo) throws Exception {
		return mvc.perform(post("/api/reuniones/{id}/asistencias/codigo", reunion).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"codigo\":\"%s\"}".formatted(codigo)));
	}

	private ResultActions marcarMiembro(JwtRequestPostProcessor token, long reunion, long miembro) throws Exception {
		return mvc.perform(post("/api/reuniones/{id}/asistencias", reunion).with(token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"miembroId\":%d}".formatted(miembro)));
	}

}
