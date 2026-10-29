package com.iglesia.api.asistencia.model;

import org.junit.jupiter.api.Test;

import static com.iglesia.api.asistencia.model.IndicadoresPrueba.HOY;
import static com.iglesia.api.asistencia.model.IndicadoresPrueba.miembro;
import static org.assertj.core.api.Assertions.assertThat;

class TipoAlertaTest {

	private static final ReglasSeguimiento REGLAS = ReglasSeguimiento.porDefecto();

	@Test
	void quienAsisteSiempreNoTieneAlertas() {
		assertThat(MiembroEvaluado.evaluar(miembro().crear(), REGLAS, HOY).alertas()).isEmpty();
	}

	@Test
	void faltasSeguidasDesdeElValorDeLaIglesia() {
		assertThat(TipoAlerta.FALTAS_SEGUIDAS.aplica(miembro().faltasSeguidas(2).crear(), REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.FALTAS_SEGUIDAS.aplica(miembro().faltasSeguidas(3).crear(), REGLAS, HOY)).isTrue();
		assertThat(TipoAlerta.FALTAS_SEGUIDAS.aplica(miembro().faltasSeguidas(2).crear(),
				new ReglasSeguimiento(2, 30, 90), HOY)).isTrue();
	}

	@Test
	void caidaCuandoBajaAlMenosLosPuntosDesdeUnaBaseDeCincuenta() {
		var bajo50Puntos = miembro().semanas5a12(10, 9).ultimas4(10, 4).crear();
		var bajo20Puntos = miembro().semanas5a12(10, 9).ultimas4(10, 7).crear();
		var baseBaja = miembro().semanas5a12(10, 4).ultimas4(10, 0).crear();
		var sinDatosRecientes = miembro().semanas5a12(10, 9).ultimas4(0, 0).crear();

		assertThat(TipoAlerta.ASISTENCIA_EN_CAIDA.aplica(bajo50Puntos, REGLAS, HOY)).isTrue();
		assertThat(TipoAlerta.ASISTENCIA_EN_CAIDA.aplica(bajo20Puntos, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.ASISTENCIA_EN_CAIDA.aplica(baseBaja, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.ASISTENCIA_EN_CAIDA.aplica(sinDatosRecientes, REGLAS, HOY)).isFalse();
	}

	@Test
	void nuevoQueVinoPocasVecesYNoVolvio() {
		var noVolvio = miembro().ingreso(HOY.minusDays(40)).diasAsistidos(1).ultimaAsistencia(HOY.minusDays(39)).crear();
		var volvio = miembro().ingreso(HOY.minusDays(40)).diasAsistidos(2).ultimaAsistencia(HOY.minusDays(5)).crear();
		var vinoMucho = miembro().ingreso(HOY.minusDays(40)).diasAsistidos(3).ultimaAsistencia(HOY.minusDays(20)).crear();
		var recienLlegado = miembro().ingreso(HOY.minusDays(5)).diasAsistidos(0).ultimaAsistencia(null).crear();
		var antiguo = miembro().ingreso(HOY.minusDays(200)).diasAsistidos(1).ultimaAsistencia(HOY.minusDays(150)).crear();
		var antiguoConCondicionNuevo = miembro().ingreso(HOY.minusDays(200))
			.nuevo()
			.diasAsistidos(0)
			.ultimaAsistencia(null)
			.crear();

		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(noVolvio, REGLAS, HOY)).isTrue();
		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(volvio, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(vinoMucho, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(recienLlegado, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(antiguo, REGLAS, HOY)).isFalse();
		assertThat(TipoAlerta.NUEVO_SIN_VOLVER.aplica(antiguoConCondicionNuevo, REGLAS, HOY)).isTrue();
	}

	@Test
	void lasAlertasVienenDeLaMasGraveALaMenosGrave() {
		var todas = miembro().ingreso(HOY.minusDays(60))
			.faltasSeguidas(5)
			.diasAsistidos(2)
			.ultimaAsistencia(HOY.minusDays(30))
			.semanas5a12(10, 9)
			.ultimas4(10, 0)
			.crear();

		assertThat(MiembroEvaluado.evaluar(todas, REGLAS, HOY).alertas()).containsExactly(TipoAlerta.FALTAS_SEGUIDAS,
				TipoAlerta.NUEVO_SIN_VOLVER, TipoAlerta.ASISTENCIA_EN_CAIDA);
	}

}
