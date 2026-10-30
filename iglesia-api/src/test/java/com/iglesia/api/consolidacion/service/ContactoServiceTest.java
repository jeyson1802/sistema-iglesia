package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.dto.RegistrarContactoRequest;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ConsolidacionPrueba;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.model.DatosContacto;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

	private static final Instant AHORA = ConsolidacionPrueba.AHORA;

	private static final Long USUARIO = 3L;

	@Mock
	private ContactoConsolidacionRepository contactos;

	@Mock
	private CasoService casos;

	@Mock
	private EquipoConsolidacionService equipo;

	@Mock
	private ResumenesDeCasos resumenes;

	@Mock
	private AmbitoConsolidacion ambito;

	@Mock
	private UsuarioActual usuarioActual;

	private ContactoService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ContactoService(contactos, casos, equipo, resumenes, ambito, usuarioActual,
				Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	@Test
	void unaFechaFuturaEsInvalidaMasAllaDeLaToleranciaDelReloj() {
		casoAbierto();

		assertThatThrownBy(() -> servicio.registrar(310L, solicitud(AHORA.plus(Duration.ofMinutes(10)),
				MedioContacto.LLAMADA, ResultadoContacto.SE_HABLO, null, null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ContactoService.FECHA_FUTURA);
	}

	@Test
	void elResultadoDebeTenerSentidoParaElMedio() {
		casoAbierto();

		assertThatThrownBy(() -> servicio
			.registrar(310L, solicitud(AHORA, MedioContacto.LLAMADA, ResultadoContacto.NO_ESTABA, null, null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ContactoService.RESULTADO_NO_APLICA);
	}

	@Test
	void elProximoContactoNoPuedeSerAnteriorAlContacto() {
		casoAbierto();

		assertThatThrownBy(() -> servicio.registrar(310L, solicitud(AHORA, MedioContacto.LLAMADA,
				ResultadoContacto.SE_HABLO, LocalDate.of(2026, 11, 2), null)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessage(ContactoService.PROXIMO_ANTERIOR);
	}

	@Test
	void enUnCasoCerradoNoSeRegistran() {
		given(casos.paraActuar(310L))
			.willReturn(ConsolidacionPrueba.caso(310L, 7L, 40L, 25L, EstadoCaso.CERRADO));

		assertThatThrownBy(() -> servicio
			.registrar(310L, solicitud(AHORA, MedioContacto.LLAMADA, ResultadoContacto.SE_HABLO, null, null)))
			.isInstanceOf(ConflictoException.class);
	}

	@Test
	void soloElCoordinadorRegistraEnNombreDeOtro() {
		casoAbierto();
		given(ambito.esCoordinador()).willReturn(false);

		assertThatThrownBy(() -> servicio
			.registrar(310L, solicitud(AHORA, MedioContacto.LLAMADA, ResultadoContacto.SE_HABLO, null, 99L)))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void elPrimerContactoPasaElCasoAEnContactoConSuProximaFecha() {
		CasoConsolidacion caso = casoAbierto();
		given(usuarioActual.id()).willReturn(Optional.of(USUARIO));
		given(contactos.save(any())).willAnswer(invocacion -> invocacion.getArgument(0));
		LocalDate proximo = LocalDate.of(2026, 11, 10);
		given(contactos.findFirstByCasoIdOrderByFechaHoraDescIdDesc(310L)).willAnswer(invocacion -> Optional.of(
				ContactoConsolidacion.nuevo(310L, new DatosContacto(AHORA,
						MedioContacto.LLAMADA, ResultadoContacto.SE_HABLO, null, proximo, 25L), USUARIO, AHORA)));

		servicio.registrar(310L, solicitud(AHORA, MedioContacto.LLAMADA, ResultadoContacto.SE_HABLO, proximo, null));

		assertThat(caso.getEstado()).isEqualTo(EstadoCaso.EN_CONTACTO);
		assertThat(caso.getProximoContacto()).isEqualTo(proximo);
	}

	@Test
	void unContactoAjenoNoSeCorrige() {
		contactoRegistradoPor(77L, AHORA);
		given(usuarioActual.id()).willReturn(Optional.of(USUARIO));

		assertThatThrownBy(() -> servicio.borrar(900L)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void pasadas24HorasElConsolidadorYaNoCorrigeSuContacto() {
		contactoRegistradoPor(USUARIO, AHORA.minus(Duration.ofHours(25)));
		given(usuarioActual.id()).willReturn(Optional.of(USUARIO));

		assertThatThrownBy(() -> servicio.borrar(900L)).isInstanceOf(ConflictoException.class);
	}

	@Test
	void elCoordinadorCorrigeCualquierContactoSinLimiteDeTiempo() {
		contactoRegistradoPor(77L, AHORA.minus(Duration.ofDays(30)));
		given(ambito.esCoordinador()).willReturn(true);
		given(casos.paraActuar(310L)).willReturn(ConsolidacionPrueba.caso(310L, 7L, 40L, 25L, EstadoCaso.EN_CONTACTO));

		servicio.borrar(900L);
	}

	private CasoConsolidacion casoAbierto() {
		CasoConsolidacion caso = ConsolidacionPrueba.caso(310L, 7L, 40L, 25L, EstadoCaso.PENDIENTE);
		given(casos.paraActuar(310L)).willReturn(caso);
		return caso;
	}

	private void contactoRegistradoPor(Long usuario, Instant cuando) {
		given(contactos.findById(900L)).willReturn(
				Optional.of(ConsolidacionPrueba.contacto(900L, 310L, usuario, cuando, ResultadoContacto.SE_HABLO)));
	}

	private static RegistrarContactoRequest solicitud(Instant fechaHora, MedioContacto medio,
			ResultadoContacto resultado, LocalDate proximo, Long consolidadorId) {
		return new RegistrarContactoRequest(fechaHora, medio, resultado, null, proximo, consolidadorId);
	}

}
