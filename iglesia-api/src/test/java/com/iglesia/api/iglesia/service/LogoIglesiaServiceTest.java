package com.iglesia.api.iglesia.service;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.error.exception.ContenidoDemasiadoGrandeException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.iglesia.component.AlmacenLogoIglesia;
import com.iglesia.api.iglesia.component.AlmacenLogoIglesia.ImagenGuardada;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.model.DatosIglesia;
import com.iglesia.api.iglesia.model.LogoIglesia;
import com.iglesia.api.iglesia.model.TipoImagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LogoIglesiaServiceTest {

	private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };

	@Mock
	private IglesiaService catalogo;

	@Mock
	private AlmacenLogoIglesia almacen;

	@Mock
	private UsuarioActual usuarioActual;

	private LogoIglesiaService servicio;

	private Iglesia iglesia;

	@BeforeEach
	void configurar() {
		servicio = new LogoIglesiaService(catalogo, almacen, usuarioActual, Clock.fixed(AHORA, ZoneOffset.UTC));
		iglesia = Iglesia.nueva(new DatosIglesia("Sion", "Sion", "Av. 1", "150132", null, null, null));
	}

	@Test
	void guardaUnaImagenReconocidaPorSuFirmaYAnotaCuandoSeSubio() {
		given(catalogo.buscar(3L)).willReturn(iglesia);

		servicio.guardar(3L, new ByteArrayInputStream(PNG));

		then(almacen).should().guardar(3L, PNG, TipoImagen.PNG);
		assertThat(iglesia.getLogoActualizadoEn()).isEqualTo(AHORA);
	}

	@Test
	void rechazaUnArchivoQueNoEsImagenAdmitidaOVacio() {
		given(catalogo.buscar(3L)).willReturn(iglesia);

		assertThatThrownBy(() -> servicio.guardar(3L, new ByteArrayInputStream("<svg/>".getBytes())))
			.isInstanceOfSatisfying(ReglaNegocioException.class, ex -> assertThat(ex.campo()).contains("logo"))
			.hasMessage(LogoIglesiaService.TIPO_NO_ADMITIDO);
		assertThatThrownBy(() -> servicio.guardar(3L, new ByteArrayInputStream(new byte[0])))
			.hasMessage(LogoIglesiaService.VACIO);
		then(almacen).should(never()).guardar(anyLong(), any(), any());
		assertThat(iglesia.tieneLogo()).isFalse();
	}

	@Test
	void rechazaUnaImagenDeMasDeUnMegabyteSinLeerlaEntera() {
		given(catalogo.buscar(3L)).willReturn(iglesia);
		byte[] grande = Arrays.copyOf(PNG, 3 * 1024 * 1024);
		ByteArrayInputStream entrada = new ByteArrayInputStream(grande);

		assertThatThrownBy(() -> servicio.guardar(3L, entrada)).isInstanceOf(ContenidoDemasiadoGrandeException.class)
			.hasMessage(LogoIglesiaService.DEMASIADO_GRANDE);
		assertThat(entrada.available()).isEqualTo(grande.length - 1024 * 1024 - 1);
	}

	@Test
	void devuelveElLogoConElInstanteDeSubidaParaElEtag() {
		iglesia.registrarLogo(AHORA);
		given(catalogo.buscar(3L)).willReturn(iglesia);
		given(almacen.leer(3L)).willReturn(Optional.of(new ImagenGuardada(PNG, "image/png")));

		LogoIglesia logo = servicio.obtener(3L);

		assertThat(logo.tipo()).isEqualTo("image/png");
		assertThat(logo.actualizadoEn()).isEqualTo(AHORA);
	}

	@Test
	void sinLogoRespondeNoEncontradoSinLeerLaTabla() {
		given(catalogo.buscar(3L)).willReturn(iglesia);

		assertThatThrownBy(() -> servicio.obtener(3L)).isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(LogoIglesiaService.SIN_LOGO);
		then(almacen).should(never()).leer(anyLong());
	}

	@Test
	void quitarBorraLaImagenYLaMarcaDelCatalogo() {
		iglesia.registrarLogo(AHORA);
		given(catalogo.buscar(3L)).willReturn(iglesia);

		servicio.quitar(3L);

		then(almacen).should().borrar(3L);
		assertThat(iglesia.tieneLogo()).isFalse();
	}

}
