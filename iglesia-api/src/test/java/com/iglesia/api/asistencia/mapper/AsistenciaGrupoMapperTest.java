package com.iglesia.api.asistencia.mapper;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.iglesia.api.asistencia.repository.AsistenciaGrupoRepository.FilaReunion;

import static org.assertj.core.api.Assertions.assertThat;

class AsistenciaGrupoMapperTest {

	private final AsistenciaGrupoMapper mapper = new AsistenciaGrupoMapper();

	@Test
	void losEsperadosSonLosQueAsistieronMasLosQueFaltaronYElPorcentajeSeRedondea() {
		var reunion = mapper.aReunion(fila(2, 1, 4));

		assertThat(reunion.esperados()).isEqualTo(3);
		assertThat(reunion.porcentaje()).isEqualTo(67);
		assertThat(reunion.visitantes()).isEqualTo(4);
	}

	@Test
	void sinIntegrantesEsperadosNoHayPorcentaje() {
		assertThat(mapper.aReunion(fila(0, 0, 2)).porcentaje()).isNull();
	}

	private static FilaReunion fila(long asistieron, long faltaron, long visitantes) {
		return new FilaReunion() {

			@Override
			public Long getReunionId() {
				return 1L;
			}

			@Override
			public LocalDate getFecha() {
				return LocalDate.of(2026, 10, 29);
			}

			@Override
			public LocalTime getHora() {
				return null;
			}

			@Override
			public long getAsistieron() {
				return asistieron;
			}

			@Override
			public long getFaltaron() {
				return faltaron;
			}

			@Override
			public long getVisitantes() {
				return visitantes;
			}

		};
	}

}
