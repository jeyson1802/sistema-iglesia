package com.iglesia.api.miembros.mapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.MiembrosPrueba;
import com.iglesia.api.niveles.mapper.NivelMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MiembroMapperTest {

	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

	private final MiembroMapper mapper = new MiembroMapper(new NivelMapper(), RELOJ);

	@Test
	void conFechaDeNacimientoLaEdadEsExacta() {
		Miembro miembro = MiembrosPrueba.miembro(1L, "Ana", null);
		ReflectionTestUtils.setField(miembro, "fechaNacimiento", LocalDate.of(1990, 10, 29));

		MiembroResponse respuesta = mapper.aRespuesta(miembro, null, null, 0);

		assertThat(respuesta.edad()).isEqualTo(35);
		assertThat(respuesta.edadEsAproximada()).isFalse();
	}

	@Test
	void conEdadAproximadaSeGuardaElAnioEstimadoYSeDevuelveAproximada() {
		Miembro miembro = MiembrosPrueba.miembro(1L, "Ana", null);
		ReflectionTestUtils.setField(miembro, "anioNacimientoEstimado", mapper.anioEstimado(30));

		MiembroResponse respuesta = mapper.aRespuesta(miembro, null, null, 0);

		assertThat(miembro.getAnioNacimientoEstimado()).isEqualTo((short) 1996);
		assertThat(respuesta.edad()).isEqualTo(30);
		assertThat(respuesta.edadEsAproximada()).isTrue();
		assertThat(respuesta.edadAproximada()).isEqualTo(30);
	}

	@Test
	void conFechaYEdadAproximadaLaExactaMandaYLaAproximadaSeDevuelveAparte() {
		Miembro miembro = MiembrosPrueba.miembro(1L, "Ana", null);
		ReflectionTestUtils.setField(miembro, "fechaNacimiento", LocalDate.of(1990, 10, 29));
		ReflectionTestUtils.setField(miembro, "anioNacimientoEstimado", mapper.anioEstimado(30));

		MiembroResponse respuesta = mapper.aRespuesta(miembro, null, null, 0);

		assertThat(respuesta.edad()).isEqualTo(35);
		assertThat(respuesta.edadEsAproximada()).isFalse();
		assertThat(respuesta.edadAproximada()).isEqualTo(30);
	}

	@Test
	void sinDatosNoHayEdad() {
		MiembroResponse respuesta = mapper.aRespuesta(MiembrosPrueba.miembro(1L, "Ana", null), null, null, 0);

		assertThat(respuesta.edad()).isNull();
		assertThat(respuesta.edadEsAproximada()).isFalse();
		assertThat(respuesta.edadAproximada()).isNull();
	}

	@Test
	void elResumenToleraUnMiembroSinUbicacion() {
		Miembro lider = MiembrosPrueba.miembro(1L, "Ana", null);

		var resumen = mapper.aResumen(MiembrosPrueba.miembro(2L, "Luis", lider), Map.of());

		assertThat(resumen.ubicacion()).isNull();
		assertThat(resumen.direccion()).isNull();
		assertThat(resumen.lider().nombre()).isEqualTo("Ana Prueba");
	}

}
