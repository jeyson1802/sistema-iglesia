package com.iglesia.api.consolidacion.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.consolidacion.model.DatosContacto;
import com.iglesia.api.consolidacion.model.DatosRonda;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;

/** Entidades de consolidación para las pruebas unitarias (con id, como si vinieran de la base). */
public final class ConsolidacionPrueba {

	public static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");

	private ConsolidacionPrueba() {
	}

	public static RondaConsolidacion ronda(Long id, String nombre, EstadoRonda estado) {
		RondaConsolidacion ronda = RondaConsolidacion
			.nueva(new DatosRonda(nombre, null, LocalDate.of(2026, 11, 2), null), 1L, AHORA);
		ReflectionTestUtils.setField(ronda, "id", id);
		ReflectionTestUtils.setField(ronda, "estado", estado);
		return ronda;
	}

	public static CasoConsolidacion caso(Long id, Long rondaId, Long miembroId, Long consolidadorId, EstadoCaso estado) {
		CasoConsolidacion caso = CasoConsolidacion.individual(miembroId, consolidadorId, 1L, AHORA);
		ReflectionTestUtils.setField(caso, "id", id);
		ReflectionTestUtils.setField(caso, "rondaId", rondaId);
		ReflectionTestUtils.setField(caso, "estado", estado);
		return caso;
	}

	public static ContactoConsolidacion contacto(Long id, Long casoId, Long registradoPor, Instant registradoEn,
			ResultadoContacto resultado) {
		ContactoConsolidacion contacto = ContactoConsolidacion.nuevo(casoId,
				new DatosContacto(registradoEn, MedioContacto.LLAMADA, resultado, null, null, 25L), registradoPor,
				registradoEn);
		ReflectionTestUtils.setField(contacto, "id", id);
		return contacto;
	}

	public static ResultadoConsolidacion resultado(Long id, String nombre, int orden, boolean activo) {
		ResultadoConsolidacion resultado = ResultadoConsolidacion.nuevo(nombre, orden);
		ReflectionTestUtils.setField(resultado, "id", id);
		resultado.cambiarEstado(activo);
		return resultado;
	}

}
