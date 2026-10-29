package com.iglesia.api.asistencia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AsistenciaMiembroServiceTest {

	@Mock
	private MiembrosParaAsistencia miembros;

	private AsistenciaMiembroService servicio;

	@BeforeEach
	void configurar() {
		servicio = new AsistenciaMiembroService(miembros);
	}

	@Test
	void devuelveElCodigoYElContenidoDelQr() {
		given(miembros.codigoDe(12L)).willReturn(new CodigoAsistencia("7K2QM9XDR4TB"));

		assertThat(servicio.codigo(12L).contenidoQr()).isEqualTo("ASIS:7K2QM9XDR4TB");
	}

	@Test
	void regenerarDevuelveElCodigoNuevo() {
		given(miembros.regenerar(12L)).willReturn(new CodigoAsistencia("AB12CD34EF56"));

		assertThat(servicio.regenerarCodigo(12L).codigo()).isEqualTo("AB12CD34EF56");
	}

}
