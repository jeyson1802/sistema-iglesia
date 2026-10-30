package com.iglesia.api.consolidacion.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadosTest {

	@ParameterizedTest
	@CsvSource({ "PLANIFICADA, EN_CURSO, true", "EN_CURSO, CERRADA, true", "PLANIFICADA, CERRADA, false",
			"EN_CURSO, PLANIFICADA, false", "CERRADA, EN_CURSO, false", "CERRADA, CERRADA, false" })
	void laRondaSoloAvanzaUnPaso(EstadoRonda origen, EstadoRonda destino, boolean valida) {
		assertThat(origen.puedePasarA(destino)).isEqualTo(valida);
	}

	@Test
	void unaRondaCerradaEsDeSoloLectura() {
		assertThat(EstadoRonda.PLANIFICADA.editable()).isTrue();
		assertThat(EstadoRonda.EN_CURSO.editable()).isTrue();
		assertThat(EstadoRonda.CERRADA.editable()).isFalse();
	}

	@Test
	void soloElCasoCerradoNoEstaAbierto() {
		assertThat(EstadoCaso.PENDIENTE.abierto()).isTrue();
		assertThat(EstadoCaso.EN_CONTACTO.abierto()).isTrue();
		assertThat(EstadoCaso.CERRADO.abierto()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "SE_HABLO, LLAMADA, true", "SE_HABLO, EN_LA_IGLESIA, true", "NO_RESPONDIO, WHATSAPP, true",
			"NO_RESPONDIO, VISITA, false", "NO_ESTABA, VISITA, true", "NO_ESTABA, LLAMADA, false",
			"DATO_INCORRECTO, VISITA, true", "DATO_INCORRECTO, EN_LA_IGLESIA, false" })
	void cadaResultadoTieneSentidoSoloParaAlgunosMedios(ResultadoContacto resultado, MedioContacto medio,
			boolean aplica) {
		assertThat(resultado.aplicaA(medio)).isEqualTo(aplica);
	}

}
