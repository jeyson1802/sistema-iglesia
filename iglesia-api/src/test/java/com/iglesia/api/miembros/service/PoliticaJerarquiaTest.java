package com.iglesia.api.miembros.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.MiembrosPrueba;
import com.iglesia.api.miembros.repository.MiembroRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PoliticaJerarquiaTest {

	private static final long IGLESIA = 1L;

	@Mock
	private MiembroRepository miembros;

	@Mock
	private IglesiaActual iglesiaActual;

	@Mock
	private ConsultaGrupos grupos;

	private PoliticaJerarquia politica;

	@BeforeEach
	void configurar() {
		politica = new PoliticaJerarquia(miembros, iglesiaActual, grupos);
	}

	@Test
	void nadieEsSuPropioLider() {
		Miembro ana = MiembrosPrueba.miembro(1L, "Ana", null);

		assertThatThrownBy(() -> politica.validarNoEsSuPropioLider(ana, 1L)).isInstanceOf(ReglaNegocioException.class)
			.hasMessage(PoliticaJerarquia.PROPIO_LIDER);
		assertThatCode(() -> politica.validarNoEsSuPropioLider(null, 1L)).doesNotThrowAnyException();
	}

	@Test
	void elPastorPrincipalNoTieneLider() {
		Miembro pastor = MiembrosPrueba.miembro(1L, "Pastor", null);
		pastor.designarPastorPrincipal();

		assertThatThrownBy(() -> politica.validarNuevoLider(pastor, MiembrosPrueba.miembro(2L, "Ana", null)))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(PoliticaJerarquia.PASTOR_SIN_LIDER);
	}

	@Test
	void unLiderDeLaPropiaRedFormaUnCiclo() {
		Miembro a = MiembrosPrueba.miembro(1L, "A", null);
		Miembro c = MiembrosPrueba.miembro(3L, "C", null);
		given(iglesiaActual.requerida()).willReturn(IGLESIA);
		given(miembros.esDeLaRed(IGLESIA, 1L, 3L)).willReturn(true);

		assertThatThrownBy(() -> politica.validarNuevoLider(a, c)).isInstanceOf(ConflictoException.class)
			.hasMessage(PoliticaJerarquia.CICLO);
	}

	@Test
	void unLiderFueraDeLaRedEsValido() {
		given(iglesiaActual.requerida()).willReturn(IGLESIA);
		given(miembros.esDeLaRed(IGLESIA, 1L, 2L)).willReturn(false);

		assertThatCode(() -> politica.validarNuevoLider(MiembrosPrueba.miembro(1L, "A", null),
				MiembrosPrueba.miembro(2L, "B", null)))
			.doesNotThrowAnyException();
	}

	@Test
	void noSeDesactivaAlPastorNiAQuienSostieneParteDelArbol() {
		Miembro pastor = MiembrosPrueba.miembro(1L, "Pastor", null);
		pastor.designarPastorPrincipal();
		assertThatThrownBy(() -> politica.validarDesactivacion(pastor)).hasMessage(PoliticaJerarquia.DESACTIVAR_PASTOR);

		given(miembros.countByLiderIdAndActivoTrue(2L)).willReturn(3L);
		assertThatThrownBy(() -> politica.validarDesactivacion(MiembrosPrueba.miembro(2L, "Lider", null)))
			.hasMessage(PoliticaJerarquia.DESACTIVAR_CON_LIDERADOS);

		given(grupos.lideraAlgunGrupoActivo(4L)).willReturn(true);
		assertThatThrownBy(() -> politica.validarDesactivacion(MiembrosPrueba.miembro(4L, "Anfitrion", null)))
			.hasMessage(PoliticaJerarquia.DESACTIVAR_CON_GRUPOS);
	}

}
