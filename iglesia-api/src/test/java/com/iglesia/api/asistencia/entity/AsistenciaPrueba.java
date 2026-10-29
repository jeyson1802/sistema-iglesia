package com.iglesia.api.asistencia.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.asistencia.model.DatosCulto;
import com.iglesia.api.asistencia.model.MedioAsistencia;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.grupos.model.DiaSemana;

/** Entidades de asistencia para las pruebas unitarias (con id, como si vinieran de la base). */
public final class AsistenciaPrueba {

	private AsistenciaPrueba() {
	}

	public static CultoProgramado culto(Long id, String nombre, DiaSemana dia, LocalTime hora) {
		CultoProgramado culto = CultoProgramado.nuevo(new DatosCulto(nombre, dia, hora));
		ReflectionTestUtils.setField(culto, "id", id);
		return culto;
	}

	public static Reunion reunion(Long id, TipoReunion tipo, String nombre, LocalDate fecha, LocalTime hora,
			Long cultoId, Long grupoId) {
		Reunion reunion = BeanUtils.instantiateClass(Reunion.class);
		ReflectionTestUtils.setField(reunion, "id", id);
		ReflectionTestUtils.setField(reunion, "tipo", tipo);
		ReflectionTestUtils.setField(reunion, "nombre", nombre);
		ReflectionTestUtils.setField(reunion, "fecha", fecha);
		ReflectionTestUtils.setField(reunion, "hora", hora);
		ReflectionTestUtils.setField(reunion, "cultoProgramadoId", cultoId);
		ReflectionTestUtils.setField(reunion, "grupoId", grupoId);
		return reunion;
	}

	public static Asistencia asistencia(Long reunionId, Long miembroId, MedioAsistencia medio, Instant cuando) {
		Asistencia asistencia = BeanUtils.instantiateClass(Asistencia.class);
		ReflectionTestUtils.setField(asistencia, "reunionId", reunionId);
		ReflectionTestUtils.setField(asistencia, "miembroId", miembroId);
		ReflectionTestUtils.setField(asistencia, "medio", medio);
		ReflectionTestUtils.setField(asistencia, "registradaEn", cuando);
		return asistencia;
	}

}
