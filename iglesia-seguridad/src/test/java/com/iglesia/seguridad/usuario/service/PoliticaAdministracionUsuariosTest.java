package com.iglesia.seguridad.usuario.service;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.model.RolBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaAdministracionUsuariosTest {

	private static final UsuarioAutenticado ADMIN_SISTEMA = new UsuarioAutenticado(1L, null, null,
			Set.of(RolBase.ADMIN_SISTEMA), Set.of());

	private static final UsuarioAutenticado ADMIN_IGLESIA_1 = new UsuarioAutenticado(2L, 1L, null,
			Set.of(RolBase.ADMIN_IGLESIA), Set.of());

	private static final UsuarioAutenticado SECRETARIA_1 = new UsuarioAutenticado(3L, 1L, null,
			Set.of(RolBase.ADMIN_IGLESIA), Set.of("usuarios.ver"));

	private static final UsuarioAutenticado SIN_IGLESIA = new UsuarioAutenticado(4L, null, null,
			Set.of(RolBase.ADMIN_IGLESIA), Set.of());

	private static final UsuarioAutenticado ADMIN_SISTEMA_EN_IGLESIA_1 = new UsuarioAutenticado(5L, 1L, null,
			Set.of(RolBase.ADMIN_SISTEMA), Set.of());

	private final PoliticaAdministracionUsuarios politica = new PoliticaAdministracionUsuarios();

	@Test
	void elAdministradorDelSistemaAdministraCualquierIglesia() {
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA, 99L, Set.of(RolBase.ADMIN_IGLESIA))).isTrue();
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA, null, Set.of(RolBase.ADMIN_SISTEMA))).isTrue();
	}

	@Test
	void dentroDeUnaIglesiaElAdministradorDelSistemaSoloAdministraEsaIglesia() {
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA_EN_IGLESIA_1, 1L, Set.of(RolBase.ADMIN_IGLESIA))).isTrue();
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA_EN_IGLESIA_1, 2L, Set.of(RolBase.ADMIN_IGLESIA))).isFalse();
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA_EN_IGLESIA_1, null, Set.of(RolBase.ADMIN_SISTEMA)))
			.isFalse();
		assertThat(politica.puedeAdministrar(ADMIN_SISTEMA_EN_IGLESIA_1, 1L, Set.of(RolBase.ADMIN_SISTEMA))).isFalse();
		assertThat(politica.iglesiaParaListado(ADMIN_SISTEMA_EN_IGLESIA_1, 2L)).isEqualTo(1L);
		assertThat(politica.iglesiaParaListado(ADMIN_SISTEMA_EN_IGLESIA_1, null)).isEqualTo(1L);
	}

	@Test
	void elAdministradorDeIglesiaSoloAdministraSuIglesia() {
		assertThat(politica.puedeAdministrar(ADMIN_IGLESIA_1, 1L, Set.of(RolBase.LIDER))).isTrue();
		assertThat(politica.puedeAdministrar(ADMIN_IGLESIA_1, 2L, Set.of(RolBase.LIDER))).isFalse();
		assertThat(politica.puedeAdministrar(ADMIN_IGLESIA_1, null, Set.of(RolBase.LIDER))).isFalse();
	}

	@Test
	void elAdministradorDeIglesiaNoOtorgaAdministradorDelSistema() {
		assertThat(politica.puedeAdministrar(ADMIN_IGLESIA_1, 1L, Set.of(RolBase.ADMIN_SISTEMA))).isFalse();
	}

	@Test
	void laPoliticaDecideSobreQuienSeActuaNoQueAccionSeHace() {
		// La acción la decide el permiso del endpoint; aquí solo cuentan la iglesia y los roles otorgados.
		assertThat(politica.puedeAdministrar(SECRETARIA_1, 1L, Set.of(RolBase.MIEMBRO))).isTrue();
		assertThat(politica.puedeAdministrar(SECRETARIA_1, 2L, Set.of(RolBase.MIEMBRO))).isFalse();
	}

	@Test
	void quienNoTieneIglesiaNiEsAdministradorDelSistemaNoAdministraANadie() {
		assertThatThrownBy(() -> politica.verificarPuedeAdministrar(SIN_IGLESIA, 1L, Set.of(RolBase.MIEMBRO)))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void elListadoDeQuienTieneIglesiaIgnoraElFiltroEnviado() {
		assertThat(politica.iglesiaParaListado(ADMIN_IGLESIA_1, 2L)).isEqualTo(1L);
		assertThat(politica.iglesiaParaListado(ADMIN_SISTEMA, 2L)).isEqualTo(2L);
		assertThat(politica.iglesiaParaListado(ADMIN_SISTEMA, null)).isNull();
		assertThat(politica.iglesiaParaListado(SECRETARIA_1, null)).isEqualTo(1L);
		assertThatThrownBy(() -> politica.iglesiaParaListado(SIN_IGLESIA, null))
			.isInstanceOf(AccessDeniedException.class);
	}

}
