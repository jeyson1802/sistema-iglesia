package com.iglesia.api.iglesia.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.mapper.IglesiaMapper;
import com.iglesia.api.iglesia.repository.IglesiaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class IglesiaServiceTest {

	private static final Instant CREADA = Instant.parse("2026-10-28T15:30:00Z");

	@Mock
	private IglesiaRepository iglesias;

	@Mock
	private UsuarioActual usuarioActual;

	private IglesiaService servicio;

	@BeforeEach
	void configurar() {
		servicio = new IglesiaService(iglesias, new IglesiaMapper(), usuarioActual);
	}

	@Test
	void listaLasIglesiasEnElOrdenDelRepositorio() {
		given(iglesias.findAllByOrderByNombreAsc()).willReturn(List.of(iglesia(2L, "Betel"), iglesia(1L, "Sion")));

		assertThat(servicio.listar()).extracting(IglesiaResponse::nombre).containsExactly("Betel", "Sion");
	}

	@Test
	void obtenerUnaIglesiaInexistenteRespondeNoEncontrada() {
		given(iglesias.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(IglesiaService.NO_ENCONTRADA);
	}

	@Test
	void creaLaIglesiaConElNombreSinEspaciosAlRededor() {
		given(iglesias.saveAndFlush(any())).willAnswer(invocacion -> conId(invocacion.getArgument(0), 5L));

		IglesiaResponse creada = servicio.crear(new GuardarIglesiaRequest("  Iglesia Central  "));

		ArgumentCaptor<Iglesia> guardada = ArgumentCaptor.forClass(Iglesia.class);
		then(iglesias).should().saveAndFlush(guardada.capture());
		assertThat(guardada.getValue().getNombre()).isEqualTo("Iglesia Central");
		assertThat(creada.id()).isEqualTo(5L);
		then(iglesias).should().existsByNombreIgnoreCase("Iglesia Central");
	}

	@Test
	void noCreaUnaIglesiaConUnNombreRepetido() {
		given(iglesias.existsByNombreIgnoreCase("Sion")).willReturn(true);

		assertThatThrownBy(() -> servicio.crear(new GuardarIglesiaRequest("Sion")))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(IglesiaService.NOMBRE_REPETIDO);
		then(iglesias).should(never()).saveAndFlush(any());
	}

	@Test
	void unAltaSimultaneaConElMismoNombreRespondeConflicto() {
		given(iglesias.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("ux_iglesia_nombre"));

		assertThatThrownBy(() -> servicio.crear(new GuardarIglesiaRequest("Sion")))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(IglesiaService.NOMBRE_REPETIDO);
	}

	@Test
	void renombraExcluyendoSuPropioNombreDeLaUnicidad() {
		Iglesia sion = iglesia(1L, "Sion");
		given(iglesias.findById(1L)).willReturn(Optional.of(sion));
		given(iglesias.saveAndFlush(sion)).willReturn(sion);

		IglesiaResponse renombrada = servicio.renombrar(1L, new GuardarIglesiaRequest(" SION "));

		assertThat(renombrada.nombre()).isEqualTo("SION");
		then(iglesias).should().existsByNombreIgnoreCaseAndIdNot("SION", 1L);
	}

	@Test
	void noRenombraConElNombreDeOtraIglesiaNiUnaIglesiaInexistente() {
		given(iglesias.findById(1L)).willReturn(Optional.of(iglesia(1L, "Sion")));
		given(iglesias.existsByNombreIgnoreCaseAndIdNot("Betel", 1L)).willReturn(true);
		given(iglesias.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.renombrar(1L, new GuardarIglesiaRequest("Betel")))
			.isInstanceOf(ConflictoException.class);
		assertThatThrownBy(() -> servicio.renombrar(9L, new GuardarIglesiaRequest("Betel")))
			.isInstanceOf(RecursoNoEncontradoException.class);
	}

	private static Iglesia iglesia(Long id, String nombre) {
		return conId(Iglesia.nueva(nombre), id);
	}

	private static Iglesia conId(Iglesia iglesia, Long id) {
		ReflectionTestUtils.setField(iglesia, "id", id);
		ReflectionTestUtils.setField(iglesia, "creadaEn", CREADA);
		return iglesia;
	}

}
