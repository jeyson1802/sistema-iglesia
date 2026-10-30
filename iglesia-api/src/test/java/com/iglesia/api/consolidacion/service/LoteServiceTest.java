package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.asistencia.service.AsistenciaParaConsolidacion;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.FiltroLoteRequest;
import com.iglesia.api.consolidacion.entity.ConsolidacionPrueba;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.model.ResultadoContacto;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

	@Mock
	private RondaService rondas;

	@Mock
	private CasoConsolidacionRepository casos;

	@Mock
	private ContactoConsolidacionRepository contactos;

	@Mock
	private AsistenciaParaConsolidacion asistencia;

	@Mock
	private MiembrosParaConsolidacion miembros;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private UsuarioActual usuarioActual;

	private LoteService servicio;

	@BeforeEach
	void configurar() {
		servicio = new LoteService(rondas, casos, contactos, asistencia, miembros, new ConsolidacionMapper(),
				iglesiaActual, usuarioActual, Clock.fixed(ConsolidacionPrueba.AHORA, ZoneOffset.UTC));
	}

	@Test
	void conPersonasElegidasYAlertaSoloEntranLasQueCumplenAmbas() {
		rondaPlanificada();
		given(asistencia.conAlerta(TipoAlerta.NUEVO_SIN_VOLVER)).willReturn(Set.of(12L, 30L));
		given(casos.idsLote(anyLong(), anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(),
				anyBoolean(), anyString(), eq(LimitesConsolidacion.LOTE_MAX))).willReturn(List.of());

		servicio.vistaPrevia(7L, new FiltroLoteRequest(CondicionMiembro.NUEVO, null, null, TipoAlerta.NUEVO_SIN_VOLVER,
				null, null, null, List.of(12L, 15L)));

		then(casos).should()
			.contarLote(eq(28L), eq(7L), eq("NUEVO"), isNull(), isNull(), isNull(), eq(false), eq(false), eq(true),
					eq("12"));
	}

	@Test
	void unLoteDeMasDeMilPersonasNoSeAgrega() {
		rondaPlanificada();
		given(casos.contarLote(anyLong(), anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(),
				anyBoolean(), anyString())).willReturn(LimitesConsolidacion.LOTE_MAX + 1L);

		assertThatThrownBy(() -> servicio.agregar(7L, new FiltroLoteRequest(null, null, null, null, null, null, null, null)))
			.isInstanceOf(ReglaNegocioException.class);
		then(casos).should(never())
			.agregarLote(anyLong(), anyLong(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean(),
					anyString(), anyLong(), any());
	}

	@Test
	void noSeQuitaDelLoteAQuienYaTieneContactos() {
		given(casos.findById(310L))
			.willReturn(Optional.of(ConsolidacionPrueba.caso(310L, 7L, 40L, 25L, EstadoCaso.EN_CONTACTO)));
		given(contactos.findFirstByCasoIdOrderByFechaHoraDescIdDesc(310L)).willReturn(Optional
			.of(ConsolidacionPrueba.contacto(900L, 310L, 3L, ConsolidacionPrueba.AHORA, ResultadoContacto.SE_HABLO)));

		assertThatThrownBy(() -> servicio.quitar(7L, 310L)).isInstanceOf(ConflictoException.class)
			.hasMessage(LoteService.CASO_CON_CONTACTOS);
	}

	private void rondaPlanificada() {
		given(rondas.editable(7L)).willReturn(ConsolidacionPrueba.ronda(7L, "R", EstadoRonda.PLANIFICADA));
		given(iglesiaActual.requerida()).willReturn(28L);
	}

}
