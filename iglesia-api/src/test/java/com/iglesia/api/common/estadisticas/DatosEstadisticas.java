package com.iglesia.api.common.estadisticas;

import java.time.LocalDate;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import com.iglesia.api.PruebasNegocio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Datos de las pruebas de estadísticas: miembros, grupos y reuniones por la API o, si no hay endpoint, por SQL. */
public final class DatosEstadisticas {

	private final MockMvc mvc;

	private final JdbcTemplate jdbc;

	private final JwtRequestPostProcessor admin;

	private final long iglesiaId;

	public DatosEstadisticas(MockMvc mvc, JdbcTemplate jdbc, long iglesiaId) {
		this.mvc = mvc;
		this.jdbc = jdbc;
		this.iglesiaId = iglesiaId;
		this.admin = PruebasNegocio.administradorDe(iglesiaId);
	}

	public long miembro(String nombres, Long liderId, LocalDate ingreso) throws Exception {
		String lider = liderId == null ? "" : ",\"liderId\":" + liderId;
		return PruebasNegocio.idDe(mvc.perform(post("/api/miembros").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombres\":\"%s\",\"apellidos\":\"Prueba\",\"fechaIngreso\":\"%s\",\"condicion\":\"NUEVO\"%s}"
				.formatted(nombres, ingreso, lider))));
	}

	public long grupo(long liderId) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/grupos").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"liderId\":%d}".formatted(liderId))));
	}

	public void integrar(long grupoId, long miembroId) throws Exception {
		mvc.perform(put("/api/grupos/{id}/miembros/{miembroId}", grupoId, miembroId).with(admin))
			.andExpect(status().is2xxSuccessful());
	}

	public long reunionDeGrupo(long grupoId, LocalDate fecha) throws Exception {
		return PruebasNegocio.idDe(mvc.perform(post("/api/reuniones").with(admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"grupoId\":%d,\"fecha\":\"%s\"}".formatted(grupoId, fecha))));
	}

	/** Un culto programado y su reunión de esa fecha (sin endpoint directo para fechas pasadas sin culto). */
	public long culto(LocalDate fecha) {
		Long cultoId = jdbc.queryForObject("""
				insert into culto_programado (iglesia_id, nombre, dia_semana, hora)
				values (?, ?, 'DOMINGO', '10:00') returning id""", Long.class, iglesiaId, "Culto " + fecha);
		return jdbc.queryForObject("""
				insert into reunion (iglesia_id, tipo, nombre, fecha, culto_programado_id, creada_por, creada_en)
				values (?, 'CULTO', 'Culto', ?, ?, 7, now()) returning id""", Long.class, iglesiaId, fecha, cultoId);
	}

	public void marcar(long reunionId, long miembroId) {
		jdbc.update("""
				insert into asistencia (iglesia_id, reunion_id, miembro_id, medio, registrada_por, registrada_en)
				values (?, ?, ?, 'MANUAL', 7, now())""", iglesiaId, reunionId, miembroId);
	}

	public JwtRequestPostProcessor admin() {
		return admin;
	}

}
