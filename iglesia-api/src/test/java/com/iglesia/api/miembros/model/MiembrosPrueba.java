package com.iglesia.api.miembros.model;

import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.miembros.entity.Miembro;

/** Miembros de prueba para las pruebas unitarias del contexto. */
public final class MiembrosPrueba {

	private MiembrosPrueba() {
	}

	public static Miembro miembro(Long id, String nombres, Miembro lider) {
		Miembro miembro = Miembro.nuevo(new DatosFicha(nombres, "Prueba", null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, lider, null, null, null));
		ReflectionTestUtils.setField(miembro, "id", id);
		return miembro;
	}

}
