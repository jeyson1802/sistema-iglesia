package com.iglesia.api.niveles.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.niveles.dto.GuardarNivelRequest;
import com.iglesia.api.niveles.dto.NivelResponse;
import com.iglesia.api.niveles.dto.OrdenarNivelesRequest;
import com.iglesia.api.niveles.entity.Nivel;
import com.iglesia.api.niveles.mapper.NivelMapper;
import com.iglesia.api.niveles.repository.NivelRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NivelServiceTest {

	@Mock
	private NivelRepository niveles;

	@Mock
	private UsuarioActual usuarioActual;

	private NivelService servicio;

	@BeforeEach
	void configurar() {
		servicio = new NivelService(niveles, new NivelMapper(), usuarioActual);
	}

	@Test
	void creaElNivelAlFinalDelOrdenConElNombreNormalizado() {
		given(niveles.findAll()).willReturn(List.of(nivel(1L, "Pastor", 1)));
		given(niveles.ultimoOrden()).willReturn(1);
		given(niveles.saveAndFlush(any())).willAnswer(invocacion -> conId(invocacion.getArgument(0), 2L));

		NivelResponse creado = servicio.crear(new GuardarNivelRequest("  Diácono   mayor "));

		assertThat(creado.nombre()).isEqualTo("Diácono mayor");
		assertThat(creado.orden()).isEqualTo(2);
	}

	@Test
	void rechazaUnNombreRepetidoAunqueCambienLasMayusculasConTilde() {
		given(niveles.findAll()).willReturn(List.of(nivel(1L, "Líder", 1)));

		assertThatThrownBy(() -> servicio.crear(new GuardarNivelRequest("LÍDER")))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(NivelService.NOMBRE_REPETIDO);
	}

	@Test
	void renombrarConElMismoNombreNoChocaConsigoMismo() {
		Nivel lider = nivel(1L, "Líder", 1);
		given(niveles.findById(1L)).willReturn(Optional.of(lider));
		given(niveles.findAll()).willReturn(List.of(lider));
		given(niveles.saveAndFlush(any())).willAnswer(invocacion -> invocacion.getArgument(0));

		assertThat(servicio.renombrar(1L, new GuardarNivelRequest("líder")).nombre()).isEqualTo("líder");
	}

	@Test
	void ordenarExigeTodosLosNivelesUnaSolaVez() {
		given(niveles.findAllByOrderByOrdenAscIdAsc()).willReturn(List.of(nivel(1L, "A", 1), nivel(2L, "B", 2)));

		assertThatThrownBy(() -> servicio.ordenar(new OrdenarNivelesRequest(List.of(1L))))
			.isInstanceOf(ReglaNegocioException.class);
		assertThatThrownBy(() -> servicio.ordenar(new OrdenarNivelesRequest(List.of(1L, 1L))))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void ordenarAsignaLaPosicionDeCadaNivel() {
		Nivel a = nivel(1L, "A", 1);
		Nivel b = nivel(2L, "B", 2);
		given(niveles.findAllByOrderByOrdenAscIdAsc()).willReturn(List.of(a, b));

		servicio.ordenar(new OrdenarNivelesRequest(List.of(2L, 1L)));

		assertThat(b.getOrden()).isEqualTo(1);
		assertThat(a.getOrden()).isEqualTo(2);
	}

	@Test
	void unNivelInactivoSoloSeConservaSiYaEstabaAsignado() {
		Nivel inactivo = nivel(3L, "Antiguo", 3);
		inactivo.cambiarEstado(false);
		given(niveles.findById(3L)).willReturn(Optional.of(inactivo));

		assertThat(servicio.asignable(3L, inactivo)).isSameAs(inactivo);
		assertThatThrownBy(() -> servicio.asignable(3L, null)).isInstanceOf(ConflictoException.class)
			.hasMessage(NivelService.INACTIVO);
	}

	@Test
	void unNivelDeOtraIglesiaNoSeEncuentraYEsUnConflicto() {
		given(niveles.findById(99L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.asignable(99L, null)).isInstanceOf(ConflictoException.class)
			.hasMessage(NivelService.INEXISTENTE);
	}

	private static Nivel nivel(Long id, String nombre, int orden) {
		return conId(Nivel.nuevo(nombre, orden), id);
	}

	private static Nivel conId(Nivel nivel, Long id) {
		ReflectionTestUtils.setField(nivel, "id", id);
		return nivel;
	}

}
