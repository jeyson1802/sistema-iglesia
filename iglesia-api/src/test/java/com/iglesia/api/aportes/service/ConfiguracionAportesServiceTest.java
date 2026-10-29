package com.iglesia.api.aportes.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.aportes.dto.GuardarConfiguracionAportesRequest;
import com.iglesia.api.aportes.dto.MonedaResponse;
import com.iglesia.api.aportes.entity.ConfiguracionAportes;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.aportes.repository.ConfiguracionAportesRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ConfiguracionAportesServiceTest {

	@Mock
	private ConfiguracionAportesRepository configuraciones;

	@Mock
	private AporteRepository aportes;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private UsuarioActual usuarioActual;

	@InjectMocks
	private ConfiguracionAportesService servicio;

	@Test
	void sinConfigurarLaMonedaEsSolesYEsEditable() {
		given(iglesiaActual.id()).willReturn(Optional.of(1L));
		given(configuraciones.findById(1L)).willReturn(Optional.empty());

		assertThat(servicio.obtener().moneda()).isEqualTo("PEN");
		assertThat(servicio.obtener().monedaEditable()).isTrue();
	}

	@Test
	void cambiaLaMonedaMientrasNoHayAportesVigentes() {
		given(iglesiaActual.id()).willReturn(Optional.of(1L));
		given(iglesiaActual.requerida()).willReturn(1L);
		given(configuraciones.findById(1L)).willReturn(Optional.empty());

		assertThat(servicio.guardar(new GuardarConfiguracionAportesRequest("USD")).moneda()).isEqualTo("USD");
		then(configuraciones).should().save(any(ConfiguracionAportes.class));
	}

	@Test
	void conAportesVigentesLaMonedaNoCambia() {
		given(iglesiaActual.id()).willReturn(Optional.of(1L));
		given(configuraciones.findById(1L)).willReturn(Optional.empty());
		given(aportes.existsByAnuladoFalse()).willReturn(true);

		assertThatThrownBy(() -> servicio.guardar(new GuardarConfiguracionAportesRequest("USD")))
			.isInstanceOf(ConflictoException.class);
		then(configuraciones).should(never()).save(any());
	}

	@Test
	void rechazaUnaMonedaNoAdmitida() {
		assertThatThrownBy(() -> servicio.guardar(new GuardarConfiguracionAportesRequest("JPY")))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ConfiguracionAportesService.MONEDA_NO_ADMITIDA);
	}

	@Test
	void listaLasMonedasConNombreEnEspanolYSimbolo() {
		MonedaResponse soles = servicio.monedas().getFirst();

		assertThat(soles.codigo()).isEqualTo("PEN");
		assertThat(soles.nombre()).containsIgnoringCase("sol");
		assertThat(soles.simbolo()).isEqualTo("S/");
	}

}
