package com.iglesia.api.iglesia.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MiIglesiaServiceTest {

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private IglesiaService catalogo;

	@Mock
	private LogoIglesiaService logos;

	private MiIglesiaService servicio;

	@BeforeEach
	void configurar() {
		servicio = new MiIglesiaService(iglesiaActual, catalogo, logos);
	}

	@Test
	void operaSiempreSobreLaIglesiaDelToken() {
		given(iglesiaActual.requerida()).willReturn(8L);
		GuardarIglesiaRequest datos = new GuardarIglesiaRequest("Sion", "Sion", "Av. 1", "150132", null, null, null);
		GuardarAparienciaRequest apariencia = new GuardarAparienciaRequest("Aura", "emerald", null, "Cinzel");
		InputStream imagen = new ByteArrayInputStream(new byte[] { 1 });

		servicio.obtener();
		servicio.actualizar(datos);
		servicio.cambiarApariencia(apariencia);
		servicio.logo();
		servicio.guardarLogo(imagen);
		servicio.quitarLogo();

		then(catalogo).should().obtener(8L);
		then(catalogo).should().actualizar(8L, datos);
		then(catalogo).should().cambiarApariencia(8L, apariencia);
		then(logos).should().obtener(8L);
		then(logos).should().guardar(8L, imagen);
		then(logos).should().quitar(8L);
	}

	@Test
	void sinIglesiaEnElTokenNoHaceNada() {
		given(iglesiaActual.requerida()).willThrow(new ReglaNegocioException("Sin iglesia"));

		assertThatThrownBy(servicio::obtener).isInstanceOf(ReglaNegocioException.class);
		then(catalogo).should(never()).obtener(anyLong());
	}

}
