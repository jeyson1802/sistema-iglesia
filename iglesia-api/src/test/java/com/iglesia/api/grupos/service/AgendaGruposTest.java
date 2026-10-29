package com.iglesia.api.grupos.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.dto.ConfiguracionIglesiaResponse;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.model.DatosGrupo;
import com.iglesia.api.grupos.model.DiaSemana;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.repository.GrupoRepository;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AgendaGruposTest {

	@Mock
	private GrupoRepository grupos;

	@Mock
	private ReferenciasMiembro miembros;

	@Mock
	private ConfiguracionIglesiaService configuracion;

	private AgendaGrupos agenda;

	@BeforeEach
	void configurar() {
		agenda = new AgendaGrupos(grupos, miembros, configuracion);
	}

	@Test
	void nombraCadaGrupoConElTipoQueConfiguroLaIglesiaYSuLider() {
		given(grupos.findByDiaReunionAndActivoTrue(DiaSemana.JUEVES)).willReturn(List.of(grupo(8L, 12L)));
		given(configuracion.obtener()).willReturn(new ConfiguracionIglesiaResponse("Hogar de paz", "Hogares de paz"));
		given(miembros.referencias(anyCollection()))
			.willReturn(Map.of(12L, new ReferenciaResponse(12L, "Ana Torres", true)));

		List<GrupoAgenda> delJueves = agenda.activosDelDia(DiaSemana.JUEVES);

		assertThat(delJueves).containsExactly(new GrupoAgenda(8L, 12L, "Hogar de paz de Ana Torres", DiaSemana.JUEVES,
				LocalTime.of(19, 30), true));
	}

	@Test
	void losGruposQueLideraUnMiembro() {
		given(grupos.findByLiderId(12L)).willReturn(List.of(grupo(8L, 12L), grupo(9L, 12L)));

		assertThat(agenda.lideradosPor(12L)).containsExactlyInAnyOrder(8L, 9L);
	}

	static Grupo grupo(Long id, Long liderId) {
		Grupo grupo = Grupo.nuevo(new DatosGrupo(liderId, null, DiaSemana.JUEVES, LocalTime.of(19, 30), null, null,
				null));
		ReflectionTestUtils.setField(grupo, "id", id);
		return grupo;
	}

}
