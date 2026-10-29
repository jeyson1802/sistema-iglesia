package com.iglesia.api.aportes.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.aportes.dto.CategoriaAporteResponse;
import com.iglesia.api.aportes.dto.GuardarCategoriaAporteRequest;
import com.iglesia.api.aportes.dto.OrdenarCategoriasAporteRequest;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.aportes.mapper.CategoriaAporteMapper;
import com.iglesia.api.aportes.repository.CategoriaAporteRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoriaAporteServiceTest {

	@Mock
	private CategoriaAporteRepository categorias;

	@Mock
	private UsuarioActual usuarioActual;

	private CategoriaAporteService servicio;

	@BeforeEach
	void configurar() {
		servicio = new CategoriaAporteService(categorias, new CategoriaAporteMapper(), usuarioActual);
	}

	@Test
	void creaLaCategoriaAlFinalConElNombreNormalizado() {
		given(categorias.findAll()).willReturn(List.of(categoria(1L, "Diezmo", true)));
		given(categorias.ultimoOrden()).willReturn(2);
		given(categorias.saveAndFlush(any())).willAnswer(invocacion -> conId(invocacion.getArgument(0), 3L));

		CategoriaAporteResponse creada = servicio.crear(new GuardarCategoriaAporteRequest("  Pro   templo ", false));

		assertThat(creada.nombre()).isEqualTo("Pro templo");
		assertThat(creada.orden()).isEqualTo(3);
		assertThat(creada.exigeAportante()).isFalse();
	}

	@Test
	void rechazaUnNombreRepetidoSinDistinguirMayusculas() {
		given(categorias.findAll()).willReturn(List.of(categoria(1L, "Ofrenda", true)));

		assertThatThrownBy(() -> servicio.crear(new GuardarCategoriaAporteRequest("OFRENDA", false)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(CategoriaAporteService.NOMBRE_REPETIDO);
	}

	@Test
	void ordenarExigeTodasLasCategoriasUnaSolaVez() {
		given(categorias.findAllByOrderByOrdenAscIdAsc())
			.willReturn(List.of(categoria(1L, "A", true), categoria(2L, "B", true)));

		assertThatThrownBy(() -> servicio.ordenar(new OrdenarCategoriasAporteRequest(List.of(1L, 1L))))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void unaCategoriaInactivaSeConservaPeroNoSeAsignaDeNuevo() {
		CategoriaAporte antigua = categoria(1L, "Antigua", false);
		given(categorias.findById(1L)).willReturn(Optional.of(antigua));

		assertThat(servicio.asignable(1L, antigua)).isSameAs(antigua);
		assertThatThrownBy(() -> servicio.asignable(1L, null)).isInstanceOf(ConflictoException.class)
			.hasMessage(CategoriaAporteService.INACTIVA);
	}

	@Test
	void unaCategoriaInexistenteEsUnConflicto() {
		given(categorias.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.asignable(9L, null)).isInstanceOf(ConflictoException.class)
			.hasMessage(CategoriaAporteService.INEXISTENTE);
	}

	static CategoriaAporte categoria(Long id, String nombre, boolean activa) {
		CategoriaAporte categoria = conId(CategoriaAporte.nueva(nombre, false, id.intValue()), id);
		categoria.cambiarEstado(activa);
		return categoria;
	}

	private static CategoriaAporte conId(CategoriaAporte categoria, Long id) {
		ReflectionTestUtils.setField(categoria, "id", id);
		return categoria;
	}

}
