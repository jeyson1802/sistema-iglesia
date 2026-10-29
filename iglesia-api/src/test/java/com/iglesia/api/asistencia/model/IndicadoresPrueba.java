package com.iglesia.api.asistencia.model;

import java.time.LocalDate;

/** Constructor de indicadores para las pruebas: por defecto, un miembro antiguo que asiste siempre. */
final class IndicadoresPrueba {

	static final LocalDate HOY = LocalDate.of(2026, 10, 29);

	private LocalDate inicio = HOY.minusYears(2);

	private boolean nuevo;

	private Asistidas ultimas4 = new Asistidas(8, 8);

	private Asistidas semanas5a12 = new Asistidas(16, 16);

	private int faltasSeguidas;

	private LocalDate ultimaAsistencia = HOY.minusDays(3);

	private long diasAsistidos = 40;

	private long miembroId = 1;

	static IndicadoresPrueba miembro() {
		return new IndicadoresPrueba();
	}

	IndicadoresPrueba id(long id) {
		miembroId = id;
		return this;
	}

	IndicadoresPrueba ingreso(LocalDate fecha) {
		inicio = fecha;
		return this;
	}

	IndicadoresPrueba nuevo() {
		nuevo = true;
		return this;
	}

	IndicadoresPrueba ultimas4(long esperadas, long asistidas) {
		ultimas4 = new Asistidas(esperadas, asistidas);
		return this;
	}

	IndicadoresPrueba semanas5a12(long esperadas, long asistidas) {
		semanas5a12 = new Asistidas(esperadas, asistidas);
		return this;
	}

	IndicadoresPrueba faltasSeguidas(int faltas) {
		faltasSeguidas = faltas;
		return this;
	}

	IndicadoresPrueba ultimaAsistencia(LocalDate fecha) {
		ultimaAsistencia = fecha;
		return this;
	}

	IndicadoresPrueba diasAsistidos(long dias) {
		diasAsistidos = dias;
		return this;
	}

	IndicadoresMiembro crear() {
		return new IndicadoresMiembro(miembroId, null, null, inicio, nuevo, ultimas4, ultimas4, semanas5a12,
				semanas5a12, faltasSeguidas, ultimaAsistencia, diasAsistidos);
	}

}
