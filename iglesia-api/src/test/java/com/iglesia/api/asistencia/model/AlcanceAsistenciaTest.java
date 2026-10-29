package com.iglesia.api.asistencia.model;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlcanceAsistenciaTest {

	@Test
	void elCompletoIncluyeTodasLasReuniones() {
		AlcanceAsistencia completo = AlcanceAsistencia.completo();

		assertThat(completo.incluye(TipoReunion.CULTO, null)).isTrue();
		assertThat(completo.incluye(TipoReunion.GRUPO, 99L)).isTrue();
		assertThat(completo.vacio()).isFalse();
	}

	@Test
	void elDeGruposSoloIncluyeLasReunionesDeEsosGrupos() {
		AlcanceAsistencia deLider = AlcanceAsistencia.deGrupos(Set.of(8L));

		assertThat(deLider.incluye(TipoReunion.GRUPO, 8L)).isTrue();
		assertThat(deLider.incluye(TipoReunion.GRUPO, 9L)).isFalse();
		assertThat(deLider.incluye(TipoReunion.CULTO, null)).isFalse();
		assertThat(deLider.incluye(TipoReunion.EVENTO, null)).isFalse();
		assertThat(AlcanceAsistencia.deGrupos(Set.of()).vacio()).isTrue();
	}

}
