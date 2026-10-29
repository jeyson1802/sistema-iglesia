package com.iglesia.api.asistencia.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.asistencia.dto.AbrirReunionRequest;
import com.iglesia.api.asistencia.dto.CrearEventoRequest;
import com.iglesia.api.asistencia.dto.FiltroReuniones;
import com.iglesia.api.asistencia.dto.ReunionHoyResponse;
import com.iglesia.api.asistencia.dto.ReunionResponse;
import com.iglesia.api.asistencia.entity.AsistenciaPrueba;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.ReunionAbierta;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.model.TotalPorReunion;
import com.iglesia.api.asistencia.repository.AsistenciaRepository;
import com.iglesia.api.asistencia.repository.CultoProgramadoRepository;
import com.iglesia.api.asistencia.repository.ReunionRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.grupos.model.DiaSemana;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.service.AgendaGrupos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReunionServiceTest {

	/** Jueves. */
	private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

	private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private ReunionRepository reuniones;

	@Mock
	private AsistenciaRepository asistencias;

	@Mock
	private CultoProgramadoRepository cultos;

	@Mock
	private CultoProgramadoService horarios;

	@Mock
	private AgendaGrupos grupos;

	@Mock
	private AmbitoAsistencia ambito;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private UsuarioActual usuarioActual;

	private ReunionService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ReunionService(reuniones, asistencias, cultos, horarios, grupos, ambito, new AsistenciaMapper(),
				iglesiaActual, usuarioActual, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void hoyJuntaLasAbiertasConLosCultosYGruposPorAbrirOrdenadosPorHora() {
		given(ambito.paraMarcar()).willReturn(AlcanceAsistencia.completo());
		Reunion oracion = AsistenciaPrueba.reunion(50L, TipoReunion.CULTO, "Oración", HOY, LocalTime.of(19, 0), 1L,
				null);
		given(reuniones.findByFecha(HOY)).willReturn(List.of(oracion));
		given(asistencias.contarPorReunion(anyList())).willReturn(List.of(new TotalPorReunion(50L, 37L)));
		given(cultos.findByDiaSemanaAndActivoTrue(DiaSemana.JUEVES)).willReturn(List.of(
				AsistenciaPrueba.culto(1L, "Oración", DiaSemana.JUEVES, LocalTime.of(19, 0)),
				AsistenciaPrueba.culto(2L, "Madrugada", DiaSemana.JUEVES, LocalTime.of(6, 0))));
		given(grupos.activosDelDia(DiaSemana.JUEVES)).willReturn(
				List.of(new GrupoAgenda(8L, 12L, "Célula de Ana Torres", DiaSemana.JUEVES, LocalTime.of(20, 0), true)));

		List<ReunionHoyResponse> hoy = servicio.hoy();

		assertThat(hoy).extracting(ReunionHoyResponse::nombre)
			.containsExactly("Madrugada", "Oración", "Célula de Ana Torres");
		assertThat(hoy.get(0).id()).isNull();
		assertThat(hoy.get(1).id()).isEqualTo(50L);
		assertThat(hoy.get(1).totalAsistentes()).isEqualTo(37L);
		assertThat(hoy.get(2).grupoId()).isEqualTo(8L);
	}

	@Test
	void elLiderSoloVeHoyLosGruposQueLidera() {
		given(ambito.paraMarcar()).willReturn(AlcanceAsistencia.deGrupos(Set.of(8L)));
		given(reuniones.findByFecha(HOY)).willReturn(
				List.of(AsistenciaPrueba.reunion(50L, TipoReunion.CULTO, "Oración", HOY, LocalTime.NOON, 1L, null)));
		given(grupos.activosDelDia(DiaSemana.JUEVES)).willReturn(List.of(
				new GrupoAgenda(8L, 12L, "Célula de Ana", DiaSemana.JUEVES, LocalTime.of(20, 0), true),
				new GrupoAgenda(9L, 13L, "Célula de Luis", DiaSemana.JUEVES, LocalTime.of(20, 0), true)));

		assertThat(servicio.hoy()).extracting(ReunionHoyResponse::grupoId).containsExactly(8L);
		then(cultos).shouldHaveNoInteractions();
	}

	@Test
	void sinAlcanceNoHayReunionesHoy() {
		given(ambito.paraMarcar()).willReturn(AlcanceAsistencia.deGrupos(Set.of()));

		assertThat(servicio.hoy()).isEmpty();
	}

	@Test
	void abrirUnCultoLaPrimeraVezLaCreaYLaSegundaDevuelveLaMisma() {
		given(ambito.paraMarcar()).willReturn(AlcanceAsistencia.completo());
		given(horarios.buscar(1L)).willReturn(AsistenciaPrueba.culto(1L, "Oración", DiaSemana.JUEVES, LocalTime.NOON));
		given(iglesiaActual.requerida()).willReturn(3L);
		given(usuarioActual.id()).willReturn(Optional.of(7L));
		given(reuniones.abrirDeCulto(3L, 1L, "Oración", HOY, LocalTime.NOON, 7L, AHORA)).willReturn(1, 0);
		given(reuniones.findByCultoProgramadoIdAndFecha(1L, HOY)).willReturn(Optional
			.of(AsistenciaPrueba.reunion(50L, TipoReunion.CULTO, "Oración", HOY, LocalTime.NOON, 1L, null)));

		ReunionAbierta primera = servicio.abrir(new AbrirReunionRequest(1L, null, HOY));
		ReunionAbierta segunda = servicio.abrir(new AbrirReunionRequest(1L, null, HOY));

		assertThat(primera.creada()).isTrue();
		assertThat(segunda.creada()).isFalse();
		assertThat(segunda.reunion().id()).isEqualTo(50L);
	}

	@Test
	void elLiderNoAbreCultosNiGruposAjenos() {
		given(ambito.paraMarcar()).willReturn(AlcanceAsistencia.deGrupos(Set.of(8L)));
		given(grupos.buscar(9L)).willReturn(
				Optional.of(new GrupoAgenda(9L, 13L, "Célula de Luis", DiaSemana.JUEVES, LocalTime.NOON, true)));

		assertThatThrownBy(() -> servicio.abrir(new AbrirReunionRequest(1L, null, HOY)))
			.isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio.abrir(new AbrirReunionRequest(null, 9L, HOY)))
			.isInstanceOf(RecursoNoEncontradoException.class);
		then(reuniones).should(never()).abrirDeGrupo(anyLong(), anyLong(), any(), any(), any(), anyLong(), any());
	}

	@Test
	void unEventoDeTresDiasCreaUnaReunionPorDia() {
		given(usuarioActual.id()).willReturn(Optional.of(7L));
		given(reuniones.saveAll(anyList())).willAnswer(invocacion -> invocacion.getArgument(0));

		List<ReunionResponse> creadas = servicio.crearEvento(
				new CrearEventoRequest("  Convención   anual ", HOY, HOY.plusDays(2), LocalTime.of(18, 0)));

		assertThat(creadas).extracting(ReunionResponse::fecha).containsExactly(HOY, HOY.plusDays(1), HOY.plusDays(2));
		assertThat(creadas).allSatisfy(reunion -> {
			assertThat(reunion.nombre()).isEqualTo("Convención anual");
			assertThat(reunion.tipo()).isEqualTo(TipoReunion.EVENTO);
		});
	}

	@Test
	void unEventoDeMasDeUnaSemanaOAlRevesSeRechaza() {
		assertThatThrownBy(() -> servicio.crearEvento(new CrearEventoRequest("Convención", HOY, HOY.plusDays(7), null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ReunionService.EVENTO_LARGO);
		assertThatThrownBy(
				() -> servicio.crearEvento(new CrearEventoRequest("Convención", HOY, HOY.minusDays(1), null)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void soloSeEliminaUnEventoSinAsistentes() {
		Reunion evento = AsistenciaPrueba.reunion(60L, TipoReunion.EVENTO, "Vigilia", HOY, null, null, null);
		Reunion culto = AsistenciaPrueba.reunion(61L, TipoReunion.CULTO, "Oración", HOY, null, 1L, null);
		given(reuniones.findById(60L)).willReturn(Optional.of(evento));
		given(reuniones.findById(61L)).willReturn(Optional.of(culto));
		given(asistencias.existsByReunionId(60L)).willReturn(true, false);

		assertThatThrownBy(() -> servicio.eliminarEvento(61L)).isInstanceOf(ConflictoException.class)
			.hasMessage(ReunionService.SOLO_EVENTOS);
		assertThatThrownBy(() -> servicio.eliminarEvento(60L)).isInstanceOf(ConflictoException.class)
			.hasMessage(ReunionService.EVENTO_CON_ASISTENTES);

		given(usuarioActual.id()).willReturn(Optional.of(7L));
		servicio.eliminarEvento(60L);
		then(reuniones).should().delete(evento);
	}

	@Test
	void elHistorialRechazaUnRangoAlReves() {
		assertThatThrownBy(() -> servicio.historial(new FiltroReuniones(HOY, HOY.minusDays(1), null, null), null))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ReunionService.RANGO_INVALIDO);
	}

	@Test
	void unaReunionFueraDelAmbitoNoSeVe() {
		given(ambito.paraVerReunion()).willReturn(AlcanceAsistencia.deGrupos(Set.of(8L)));
		given(reuniones.findById(50L)).willReturn(Optional
			.of(AsistenciaPrueba.reunion(50L, TipoReunion.GRUPO, "Célula de Luis", HOY, null, null, 9L)));

		assertThatThrownBy(() -> servicio.obtener(50L)).isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(ReunionService.NO_ENCONTRADA);
	}

}
