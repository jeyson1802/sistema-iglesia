package com.iglesia.api.asistencia.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

import com.iglesia.api.asistencia.dto.FiltroOcasiones;
import com.iglesia.api.asistencia.dto.FiltroSeguimiento;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.mapper.SeguimientoMapper;
import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.AlcanceSeguimiento;
import com.iglesia.api.asistencia.model.ReglasSeguimiento;
import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.asistencia.repository.ReunionRepository;
import com.iglesia.api.asistencia.repository.SeguimientoRepository;
import com.iglesia.api.asistencia.repository.SeguimientoRepository.FilaIndicadores;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.service.AgendaGrupos;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeguimientoAsistenciaServiceTest {

	private static final long IGLESIA = 5L;

	/** Jueves: la semana empieza el lunes 26 y se analizan 16 semanas. */
	private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

	private static final LocalDate DESDE_ANALISIS = LocalDate.of(2026, 7, 13);

	@Mock
	private SeguimientoRepository consultas;

	@Mock
	private ReunionRepository reuniones;

	@Mock
	private ConfiguracionSeguimientoService configuracion;

	@Mock
	private MiembrosParaAsistencia miembros;

	@Mock
	private AgendaGrupos grupos;

	@Mock
	private AmbitoAsistencia ambito;

	@Mock
	private UsuarioActual usuarioActual;

	@Mock
	private IglesiaActual iglesiaActual;

	private SeguimientoAsistenciaService servicio;

	@BeforeEach
	void configurar() {
		given(iglesiaActual.requerida()).willReturn(IGLESIA);
		given(configuracion.reglas()).willReturn(ReglasSeguimiento.porDefecto());
		servicio = new SeguimientoAsistenciaService(consultas, reuniones, configuracion, miembros, grupos, ambito,
				new SeguimientoMapper(), new AsistenciaMapper(), usuarioActual, iglesiaActual,
				Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void laListaTraeSoloConAlertasPorRiesgoYPaginada() {
		given(ambito.paraSeguimiento()).willReturn(AlcanceSeguimiento.todaLaIglesia());
		given(consultas.indicadores(eq(IGLESIA), eq(HOY), eq(DESDE_ANALISIS), isNull(), isNull(), isNull(), eq(false)))
			.willReturn(List.of(fila(1, 0), fila(2, 4), fila(3, 7)));
		given(miembros.porIds(any())).willReturn(Map.of(3L, new MiembroAsistente(3L, "Ana", "Líder", null, true)));

		var pagina = servicio.lista(new FiltroSeguimiento(null, null, null, null), PageRequest.of(0, 1));

		assertThat(pagina.getTotalElements()).isEqualTo(2);
		assertThat(pagina.getContent()).singleElement().satisfies(persona -> {
			assertThat(persona.miembroId()).isEqualTo(3L);
			assertThat(persona.nombreCompleto()).isEqualTo("Ana");
			assertThat(persona.alertas()).containsExactly(TipoAlerta.FALTAS_SEGUIDAS);
		});
	}

	@Test
	void conTodosIncluyeTambienAQuienesNoTienenAlertas() {
		given(ambito.paraSeguimiento()).willReturn(AlcanceSeguimiento.todaLaIglesia());
		given(consultas.indicadores(anyLong(), any(), any(), any(), any(), any(), anyBoolean()))
			.willReturn(List.of(fila(1, 0), fila(2, 4)));

		var pagina = servicio.lista(new FiltroSeguimiento(null, null, null, true), PageRequest.of(0, 10));

		assertThat(pagina.getContent()).extracting(p -> p.miembroId()).containsExactly(2L, 1L);
	}

	@Test
	void elLiderVeSuRedYSinAlcanceNoSeConsulta() {
		given(ambito.paraSeguimiento()).willReturn(AlcanceSeguimiento.redDe(7L));
		servicio.conteos(null, null);
		verify(consultas).indicadores(IGLESIA, HOY, DESDE_ANALISIS, null, null, 7L, false);

		given(ambito.paraSeguimiento()).willReturn(AlcanceSeguimiento.ninguno());
		assertThat(servicio.conteos(null, null).conAlertas()).isZero();
	}

	@Test
	void unMiembroFueraDeSuRedRespondeNoEncontrado() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(false);
		given(miembros.visible(12L)).willReturn(false);
		given(miembros.porId(12L)).willReturn(Optional.of(new MiembroAsistente(12L, "Ana", null, null, true)));

		assertThatThrownBy(() -> servicio.delMiembro(12L)).isInstanceOf(RecursoNoEncontradoException.class);
		verifyNoInteractions(consultas);
	}

	@Test
	void elResumenDelMiembroTraeDoceSemanasAunqueNoHayaOcasiones() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(true);
		given(miembros.porId(12L)).willReturn(Optional.of(new MiembroAsistente(12L, "Ana", null, null, true)));

		var resumen = servicio.delMiembro(12L);

		assertThat(resumen.semanas()).hasSize(12);
		assertThat(resumen.semanas().getLast().inicio()).isEqualTo(LocalDate.of(2026, 10, 26));
		assertThat(resumen.porcentaje()).isNull();
		assertThat(resumen.alertas()).isEmpty();
	}

	@Test
	void lasOcasionesConRangoInvertidoRespondenSolicitudInvalida() {
		given(usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER)).willReturn(true);
		given(miembros.porId(12L)).willReturn(Optional.of(new MiembroAsistente(12L, "Ana", null, null, true)));

		assertThatThrownBy(() -> servicio.ocasiones(12L,
				new FiltroOcasiones(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), null), PageRequest.of(0, 10)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void unGrupoQueNoLideraRespondeNoEncontrado() {
		given(grupos.buscar(8L)).willReturn(Optional.of(new GrupoAgenda(8L, 3L, "Célula", null, null, true)));
		given(ambito.paraConsultar()).willReturn(AlcanceAsistencia.deGrupos(java.util.Set.of(9L)));

		assertThatThrownBy(() -> servicio.delGrupo(8L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	/** Miembro antiguo con asistencia perfecta salvo sus últimas {@code faltas} ocasiones. */
	private static FilaIndicadores fila(long id, int faltas) {
		return new FilaIndicadores() {

			public Long getMiembroId() {
				return id;
			}

			public Long getGrupoId() {
				return null;
			}

			public Long getLiderId() {
				return null;
			}

			public LocalDate getInicio() {
				return HOY.minusYears(1);
			}

			public boolean getNuevo() {
				return false;
			}

			public long getEsperadas4() {
				return 8;
			}

			public long getAsistidas4() {
				return 8;
			}

			public long getEsperadas8() {
				return 16;
			}

			public long getAsistidas8() {
				return 16 - faltas;
			}

			public long getEsperadas5a12() {
				return 16;
			}

			public long getAsistidas5a12() {
				return 16;
			}

			public long getEsperadas9a16() {
				return 16;
			}

			public long getAsistidas9a16() {
				return 16;
			}

			public int getFaltasSeguidas() {
				return faltas;
			}

			public LocalDate getUltimaAsistencia() {
				return HOY.minusDays(faltas * 3L);
			}

			public long getDiasAsistidos() {
				return 50;
			}

		};
	}

}
