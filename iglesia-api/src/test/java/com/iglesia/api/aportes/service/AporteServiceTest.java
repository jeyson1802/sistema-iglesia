package com.iglesia.api.aportes.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.aportes.dto.AnularAporteRequest;
import com.iglesia.api.aportes.dto.AporteResponse;
import com.iglesia.api.aportes.dto.GuardarAporteRequest;
import com.iglesia.api.aportes.entity.Aporte;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.aportes.mapper.AporteMapper;
import com.iglesia.api.aportes.mapper.CategoriaAporteMapper;
import com.iglesia.api.aportes.model.AlcanceAportes;
import com.iglesia.api.aportes.model.DatosAporte;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AporteServiceTest {

	private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");

	private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

	@Mock
	private AporteRepository aportes;

	@Mock
	private CategoriaAporteService categorias;

	@Mock
	private ReferenciasMiembro miembros;

	@Mock
	private ConsultaAportes consulta;

	@Mock
	private UsuarioActual usuarioActual;

	private AporteService servicio;

	@BeforeEach
	void configurar() {
		servicio = new AporteService(aportes, new AporteMapper(new CategoriaAporteMapper()), categorias, miembros,
				consulta, usuarioActual, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void registraUnaOfrendaAnonimaYQuitaElNumeroDeOperacionDelEfectivo() {
		CategoriaAporte ofrenda = CategoriaAporteServiceTest.categoria(2L, "Ofrenda", true);
		given(categorias.asignable(2L, null)).willReturn(ofrenda);
		given(usuarioActual.id()).willReturn(Optional.of(7L));
		given(aportes.save(any())).willAnswer(invocacion -> invocacion.getArgument(0));

		AporteResponse creado = servicio.crear(new GuardarAporteRequest(HOY, null, 2L, new BigDecimal("20.00"),
				MedioAporte.EFECTIVO, "123", "  Por mi familia\ny mi trabajo ", "   "));

		assertThat(creado.miembro()).isNull();
		assertThat(creado.numeroOperacion()).isNull();
		assertThat(creado.peticion()).isEqualTo("Por mi familia\ny mi trabajo");
		assertThat(creado.observacion()).isNull();
		ArgumentCaptor<Aporte> guardado = ArgumentCaptor.forClass(Aporte.class);
		then(aportes).should().save(guardado.capture());
		assertThat(guardado.getValue().getRegistradoPor()).isEqualTo(7L);
		assertThat(guardado.getValue().getRegistradoEn()).isEqualTo(AHORA);
	}

	@Test
	void unaCategoriaQueExigeAportanteNoAdmiteAnonimos() {
		CategoriaAporte diezmo = CategoriaAporte.nueva("Diezmo", true, 1);
		given(categorias.asignable(1L, null)).willReturn(diezmo);

		assertThatThrownBy(() -> servicio.crear(new GuardarAporteRequest(HOY, null, 1L, BigDecimal.TEN,
				MedioAporte.YAPE, null, null, null)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(AporteService.EXIGE_APORTANTE);
	}

	@Test
	void corrigeUnAporteYGuardaQuienLoModifico() {
		Aporte aporte = aporte(false);
		given(aportes.findWithCategoriaById(5L)).willReturn(Optional.of(aporte));
		given(categorias.asignable(2L, aporte.getCategoria())).willReturn(aporte.getCategoria());
		given(usuarioActual.id()).willReturn(Optional.of(8L));
		given(miembros.referencias(anyCollection())).willReturn(Collections.emptyMap());

		servicio.actualizar(5L, new GuardarAporteRequest(HOY, 3L, 2L, new BigDecimal("30.00"), MedioAporte.PLIN,
				"OP-1", null, null));

		assertThat(aporte.getMonto()).isEqualByComparingTo("30.00");
		assertThat(aporte.getModificadoPor()).isEqualTo(8L);
		assertThat(aporte.getModificadoEn()).isEqualTo(AHORA);
		then(miembros).should().validarAsignable(3L, null, "miembroId");
	}

	@Test
	void unAporteAnuladoNoSeCorrigeNiSeVuelveAAnular() {
		given(aportes.findWithCategoriaById(5L)).willReturn(Optional.of(aporte(true)));

		assertThatThrownBy(() -> servicio.actualizar(5L, new GuardarAporteRequest(HOY, null, 2L, BigDecimal.ONE,
				MedioAporte.EFECTIVO, null, null, null)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(AporteService.ANULADO);
		assertThatThrownBy(() -> servicio.anular(5L, new AnularAporteRequest("Otra vez")))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(AporteService.YA_ANULADO);
	}

	@Test
	void anulaConElMotivoNormalizado() {
		Aporte aporte = aporte(false);
		given(aportes.findWithCategoriaById(5L)).willReturn(Optional.of(aporte));
		given(usuarioActual.id()).willReturn(Optional.of(8L));

		AporteResponse anulado = servicio.anular(5L, new AnularAporteRequest("  Registrado   dos veces "));

		assertThat(anulado.anulado()).isTrue();
		assertThat(anulado.motivoAnulacion()).isEqualTo("Registrado dos veces");
	}

	@Test
	void unAporteFueraDelAmbitoNoExiste() {
		Aporte ajeno = aporte(false);
		ReflectionTestUtils.setField(ajeno, "miembroId", 9L);
		given(aportes.findWithCategoriaById(5L)).willReturn(Optional.of(ajeno));
		given(consulta.alcance()).willReturn(AlcanceAportes.soloDe(3L));

		assertThatThrownBy(() -> servicio.obtener(5L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	private static Aporte aporte(boolean anulado) {
		CategoriaAporte ofrenda = CategoriaAporteServiceTest.categoria(2L, "Ofrenda", true);
		Aporte aporte = Aporte.nuevo(new DatosAporte(HOY, null, BigDecimal.TEN, MedioAporte.EFECTIVO, null, null,
				null), ofrenda, 7L, AHORA);
		ReflectionTestUtils.setField(aporte, "id", 5L);
		if (anulado) {
			aporte.anular("Duplicado", 7L, AHORA);
		}
		return aporte;
	}

}
