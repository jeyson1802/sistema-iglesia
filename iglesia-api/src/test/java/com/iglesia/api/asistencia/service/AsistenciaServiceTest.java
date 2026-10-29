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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.iglesia.api.asistencia.dto.MiembroReunionResponse;
import com.iglesia.api.asistencia.entity.AsistenciaPrueba;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.Marca;
import com.iglesia.api.asistencia.model.MedioAsistencia;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.repository.AsistenciaRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class AsistenciaServiceTest {

	private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

	private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

	private static final CodigoAsistencia CODIGO = new CodigoAsistencia("7K2QM9XDR4TB");

	private static final MiembroAsistente ANA = new MiembroAsistente(12L, "Ana Torres", "Líder", 8L, true);

	private static final Reunion CULTO = AsistenciaPrueba.reunion(50L, TipoReunion.CULTO, "Oración", HOY, null, 1L,
			null);

	@Mock
	private AsistenciaRepository asistencias;

	@Mock
	private ReunionService reuniones;

	@Mock
	private MiembrosParaAsistencia miembros;

	@Mock
	private ReferenciasMiembro referencias;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private UsuarioActual usuarioActual;

	private AsistenciaService servicio;

	@BeforeEach
	void configurar() {
		servicio = new AsistenciaService(asistencias, reuniones, miembros, referencias, new AsistenciaMapper(),
				iglesiaActual, usuarioActual, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void marcaConElQrYDespuesRespondeYaRegistradoSinNombresEnLosLogs(CapturedOutput salida) {
		abierta(CULTO);
		given(miembros.porCodigo(CODIGO)).willReturn(Optional.of(ANA));
		given(iglesiaActual.requerida()).willReturn(3L);
		given(usuarioActual.id()).willReturn(Optional.of(7L));
		given(asistencias.marcar(3L, 50L, 12L, "QR", 7L, AHORA)).willReturn(1, 0);
		given(asistencias.findByReunionIdAndMiembroId(50L, 12L))
			.willReturn(Optional.of(AsistenciaPrueba.asistencia(50L, 12L, MedioAsistencia.QR, AHORA)));
		given(asistencias.countByReunionId(50L)).willReturn(38L);

		Marca primera = servicio.marcarPorCodigo(50L, "ASIS:7K2QM9XDR4TB");
		Marca segunda = servicio.marcarPorCodigo(50L, "7k2q-m9xd-r4tb");

		assertThat(primera.nueva()).isTrue();
		assertThat(primera.respuesta().yaRegistrado()).isFalse();
		assertThat(primera.respuesta().nombreCompleto()).isEqualTo("Ana Torres");
		assertThat(primera.respuesta().totalAsistentes()).isEqualTo(38L);
		assertThat(segunda.nueva()).isFalse();
		assertThat(segunda.respuesta().yaRegistrado()).isTrue();
		assertThat(salida.getOut()).contains("Asistencia marcada reunionId=50 miembroId=12 medio=QR")
			.doesNotContain("Ana")
			.doesNotContain(CODIGO.valor());
	}

	@Test
	void unQrAjenoOUnCodigoDeOtraIglesiaSeRechazanSinRegistrarLoLeido(CapturedOutput salida) {
		abierta(CULTO);
		given(usuarioActual.id()).willReturn(Optional.of(7L));
		given(miembros.porCodigo(CODIGO)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.marcarPorCodigo(50L, "https://pago.example/qr?monto=10"))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(AsistenciaService.NO_ES_CODIGO);
		assertThatThrownBy(() -> servicio.marcarPorCodigo(50L, CODIGO.contenidoQr()))
			.isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(AsistenciaService.CODIGO_NO_ENCONTRADO);
		assertThat(salida.getOut()).doesNotContain("pago.example").doesNotContain(CODIGO.valor());
	}

	@Test
	void unMiembroInactivoNoSeMarca() {
		abierta(CULTO);
		given(miembros.porId(12L)).willReturn(Optional.of(new MiembroAsistente(12L, "Ana", null, null, false)));

		assertThatThrownBy(() -> servicio.marcarMiembro(50L, 12L)).isInstanceOf(ConflictoException.class)
			.hasMessage(AsistenciaService.MIEMBRO_INACTIVO);
	}

	@Test
	void noSeTomaAsistenciaEnUnaReunionFutura() {
		Reunion manana = AsistenciaPrueba.reunion(60L, TipoReunion.EVENTO, "Convención", HOY.plusDays(1), null, null,
				null);
		given(reuniones.paraMarcar(60L)).willReturn(manana);
		given(reuniones.hoyEnLaIglesia()).willReturn(HOY);

		assertThatThrownBy(() -> servicio.marcarMiembro(60L, 12L)).isInstanceOf(ReglaNegocioException.class)
			.hasMessage(AsistenciaService.REUNION_FUTURA);
		then(asistencias).shouldHaveNoInteractions();
	}

	@Test
	void quitarUnaMarcaQueNoExisteRespondeNoEncontrado() {
		given(asistencias.findByReunionIdAndMiembroId(50L, 12L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.quitar(50L, 12L)).isInstanceOf(RecursoNoEncontradoException.class)
			.hasMessage(AsistenciaService.NO_MARCADO);
	}

	@Test
	void losIntegrantesVanPrimeroYLuegoLosQueVinieronDeOtroGrupo() {
		Reunion celula = AsistenciaPrueba.reunion(70L, TipoReunion.GRUPO, "Célula de Ana", HOY, null, null, 8L);
		given(reuniones.paraMarcar(70L)).willReturn(celula);
		given(referencias.integrantesDe(8L)).willReturn(List.of(new IntegranteGrupo(12L, "Ana Torres", "Líder", 8L),
				new IntegranteGrupo(13L, "Beto Ruiz", null, 8L)));
		given(asistencias.miembrosDe(70L)).willReturn(List.of(12L, 40L));
		given(miembros.porIds(anyCollection()))
			.willReturn(Map.of(40L, new MiembroAsistente(40L, "Carla Díaz", null, 9L, true)));

		List<MiembroReunionResponse> lista = servicio.integrantes(70L);

		assertThat(lista).extracting(MiembroReunionResponse::miembroId).containsExactly(12L, 13L, 40L);
		assertThat(lista).extracting(MiembroReunionResponse::presente).containsExactly(true, false, true);
		assertThat(lista).extracting(MiembroReunionResponse::integrante).containsExactly(true, true, false);
	}

	@Test
	void soloLasReunionesDeGrupoTienenIntegrantes() {
		given(reuniones.paraMarcar(50L)).willReturn(CULTO);

		assertThatThrownBy(() -> servicio.integrantes(50L)).isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void buscarConMenosDeDosLetrasNoConsulta() {
		given(reuniones.paraMarcar(50L)).willReturn(CULTO);

		assertThat(servicio.buscarMiembros(50L, " a ")).isEmpty();
		then(miembros).shouldHaveNoInteractions();
	}

	private void abierta(Reunion reunion) {
		given(reuniones.paraMarcar(reunion.getId())).willReturn(reunion);
		given(reuniones.hoyEnLaIglesia()).willReturn(HOY);
	}

}
