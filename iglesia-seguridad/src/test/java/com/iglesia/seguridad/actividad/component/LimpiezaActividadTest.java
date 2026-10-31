package com.iglesia.seguridad.actividad.component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.iglesia.seguridad.actividad.config.RetencionActividadProperties;
import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LimpiezaActividadTest {

	@Test
	void borraLoAnteriorALaRetencion() {
		ActividadUsuarioRepository actividades = mock(ActividadUsuarioRepository.class);
		Instant ahora = Instant.parse("2026-10-30T12:00:00Z");
		new LimpiezaActividad(actividades, new RetencionActividadProperties(25),
				Clock.fixed(ahora, ZoneId.systemDefault()))
			.limpiar();

		verify(actividades).borrarAnterioresA(LocalDate.ofInstant(ahora, ZoneId.systemDefault()).minusMonths(25));
	}

}
