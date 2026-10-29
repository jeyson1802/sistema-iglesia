package com.iglesia.api.iglesia.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.FiltroIglesias;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.mapper.IglesiaMapper;
import com.iglesia.api.iglesia.model.DatosIglesia;
import com.iglesia.api.iglesia.model.IglesiaCreada;
import com.iglesia.api.iglesia.repository.IglesiaRepository;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.dto.UbigeoResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class IglesiaServiceTest {

	private static final Instant CREADA = Instant.parse("2026-10-28T15:30:00Z");

	private static final String DISTRITO = "150132";

	private static final UbicacionResponse SJL = new UbicacionResponse(new UbigeoResponse("15", "LIMA"),
			new UbigeoResponse("1501", "LIMA"), new UbigeoResponse(DISTRITO, "SAN JUAN DE LURIGANCHO"));

	@Mock
	private IglesiaRepository iglesias;

	@Mock
	private UbigeoService ubigeo;

	@Mock
	private UsuarioActual usuarioActual;

	@Mock
	private ApplicationEventPublisher eventos;

	private IglesiaService servicio;

	@BeforeEach
	void configurar() {
		servicio = new IglesiaService(iglesias, new IglesiaMapper(), ubigeo, usuarioActual, eventos);
	}

	@Test
	@SuppressWarnings("unchecked")
	void listaUnaPaginaFiltradaConLasUbicacionesEnUnaSolaConsulta() {
		Iglesia betel = iglesia(2L, "Betel");
		given(iglesias.findAll(any(Specification.class), any(PageRequest.class)))
			.willReturn(new PageImpl<>(List.of(betel, iglesia(1L, "Sion"))));
		given(ubigeo.ubicaciones(anyList())).willReturn(Map.of(DISTRITO, SJL));

		var pagina = servicio.listar(new FiltroIglesias("lima", "15", true), PageRequest.of(0, 10));

		assertThat(pagina.getContent()).extracting(IglesiaResponse::nombreComercial).containsExactly("Betel", "Sion");
		assertThat(pagina.getContent().getFirst().ubicacion()).isEqualTo(SJL);
		then(ubigeo).should().ubicaciones(List.of(DISTRITO, DISTRITO));
	}

	@Test
	void obtenerUnaIglesiaInexistenteRespondeNoEncontrada() {
		given(iglesias.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(IglesiaService.NO_ENCONTRADA);
	}

	@Test
	void creaLaIglesiaConLosTextosNormalizadosYPublicaElEvento() {
		given(iglesias.saveAndFlush(any())).willAnswer(invocacion -> conId(invocacion.getArgument(0), 5L));

		IglesiaResponse creada = servicio.crear(new GuardarIglesiaRequest("  Iglesia   Central  ",
				" Iglesia Evangélica  Central ", " Av. Lima 1 ", DISTRITO, "  ", "", " https://central.org "));

		ArgumentCaptor<Iglesia> guardada = ArgumentCaptor.forClass(Iglesia.class);
		then(iglesias).should().saveAndFlush(guardada.capture());
		assertThat(guardada.getValue().getNombreComercial()).isEqualTo("Iglesia Central");
		assertThat(guardada.getValue().getDenominacion()).isEqualTo("Iglesia Evangélica Central");
		assertThat(guardada.getValue().getTelefono()).isNull();
		assertThat(guardada.getValue().getCorreo()).isNull();
		assertThat(guardada.getValue().getSitioWeb()).isEqualTo("https://central.org");
		assertThat(creada.id()).isEqualTo(5L);
		assertThat(creada.apariencia()).isEqualTo(new AparienciaResponse("Aura", "emerald", null, "Playfair Display"));
		then(ubigeo).should().validarDistrito(DISTRITO, "distritoCodigo");
		then(eventos).should().publishEvent(new IglesiaCreada(5L));
	}

	@Test
	void unDistritoInexistenteNoSeGuarda() {
		willThrow(new ReglaNegocioException("El distrito no existe en el ubigeo", "distritoCodigo")).given(ubigeo)
			.validarDistrito("999999", "distritoCodigo");

		assertThatThrownBy(() -> servicio.crear(solicitud("Sion", "999999")))
			.isInstanceOf(ReglaNegocioException.class);
		then(iglesias).should(never()).saveAndFlush(any());
	}

	@Test
	void noCreaUnaIglesiaConUnNombreComercialRepetido() {
		given(iglesias.existsByNombreComercialIgnoreCase("Sion")).willReturn(true);

		assertThatThrownBy(() -> servicio.crear(solicitud("Sion", DISTRITO)))
			.isInstanceOfSatisfying(ConflictoException.class,
					ex -> assertThat(ex.campo()).contains(IglesiaService.CAMPO_NOMBRE_COMERCIAL))
			.hasMessage(IglesiaService.NOMBRE_REPETIDO);
		then(iglesias).should(never()).saveAndFlush(any());
	}

	@Test
	void unAltaSimultaneaConElMismoNombreRespondeConflicto() {
		given(iglesias.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("ux_iglesia_nombre_comercial"));

		assertThatThrownBy(() -> servicio.crear(solicitud("Sion", DISTRITO))).isInstanceOf(ConflictoException.class)
			.hasMessage(IglesiaService.NOMBRE_REPETIDO);
	}

	@Test
	void actualizaExcluyendoSuPropioNombreDeLaUnicidad() {
		Iglesia sion = iglesia(1L, "Sion");
		given(iglesias.findById(1L)).willReturn(Optional.of(sion));
		given(iglesias.saveAndFlush(sion)).willReturn(sion);

		IglesiaResponse actualizada = servicio.actualizar(1L, solicitud(" SION ", DISTRITO));

		assertThat(actualizada.nombreComercial()).isEqualTo("SION");
		then(iglesias).should().existsByNombreComercialIgnoreCaseAndIdNot("SION", 1L);
	}

	@Test
	void noActualizaConElNombreDeOtraIglesiaNiUnaIglesiaInexistente() {
		given(iglesias.findById(1L)).willReturn(Optional.of(iglesia(1L, "Sion")));
		given(iglesias.existsByNombreComercialIgnoreCaseAndIdNot("Betel", 1L)).willReturn(true);
		given(iglesias.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.actualizar(1L, solicitud("Betel", DISTRITO)))
			.isInstanceOf(ConflictoException.class);
		assertThatThrownBy(() -> servicio.actualizar(9L, solicitud("Betel", DISTRITO)))
			.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void cambiaLaAparienciaConValoresAdmitidos() {
		Iglesia sion = iglesia(1L, "Sion");
		given(iglesias.findById(1L)).willReturn(Optional.of(sion));

		AparienciaResponse apariencia = servicio.cambiarApariencia(1L,
				new GuardarAparienciaRequest("Lara", "noir", " ", "Cinzel"));

		assertThat(apariencia).isEqualTo(new AparienciaResponse("Lara", "noir", null, "Cinzel"));
		assertThat(sion.getApariencia().preset()).isEqualTo("Lara");
	}

	@Test
	void rechazaCadaValorDeAparienciaNoAdmitidoJuntoASuCampo() {
		given(iglesias.findById(1L)).willReturn(Optional.of(iglesia(1L, "Sion")));

		assertThatThrownBy(() -> servicio.cambiarApariencia(1L, apariencia("Material", "emerald", null, "Cinzel")))
			.isInstanceOfSatisfying(ReglaNegocioException.class, ex -> assertThat(ex.campo()).contains("preset"));
		assertThatThrownBy(() -> servicio.cambiarApariencia(1L, apariencia("Aura", "marron", null, "Cinzel")))
			.isInstanceOfSatisfying(ReglaNegocioException.class,
					ex -> assertThat(ex.campo()).contains("colorPrimario"));
		assertThatThrownBy(() -> servicio.cambiarApariencia(1L, apariencia("Aura", "emerald", "azul", "Cinzel")))
			.isInstanceOfSatisfying(ReglaNegocioException.class,
					ex -> assertThat(ex.campo()).contains("colorSuperficie"));
		assertThatThrownBy(() -> servicio.cambiarApariencia(1L, apariencia("Aura", "emerald", null, "Arial")))
			.isInstanceOfSatisfying(ReglaNegocioException.class, ex -> assertThat(ex.campo()).contains("tipografia"));
	}

	private static GuardarAparienciaRequest apariencia(String preset, String color, String superficie,
			String tipografia) {
		return new GuardarAparienciaRequest(preset, color, superficie, tipografia);
	}

	private static GuardarIglesiaRequest solicitud(String nombre, String distrito) {
		return new GuardarIglesiaRequest(nombre, "Denominación de " + nombre.strip(), "Av. Lima 1", distrito, null,
				null, null);
	}

	private static Iglesia iglesia(Long id, String nombre) {
		return conId(Iglesia.nueva(new DatosIglesia(nombre, nombre, "Av. Lima 1", DISTRITO, null, null, null)), id);
	}

	private static Iglesia conId(Iglesia iglesia, Long id) {
		ReflectionTestUtils.setField(iglesia, "id", id);
		ReflectionTestUtils.setField(iglesia, "creadaEn", CREADA);
		return iglesia;
	}

}
