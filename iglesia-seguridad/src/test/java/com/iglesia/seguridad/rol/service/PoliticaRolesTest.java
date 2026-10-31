package com.iglesia.seguridad.rol.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaRolesTest {

	private static final UsuarioAutenticado ADMIN_IGLESIA_1 = new UsuarioAutenticado(2L, 1L, null,
			Set.of(RolBase.ADMIN_IGLESIA), Set.of("roles.ver", "roles.administrar"));

	private static final UsuarioAutenticado ADMIN_SISTEMA = new UsuarioAutenticado(1L, null, null,
			Set.of(RolBase.ADMIN_SISTEMA), Set.of("roles.ver"));

	private static final Rol SECRETARIA_1 = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA,
			"miembros.ver");

	private static final Rol SECRETARIA_2 = RolesPrueba.personalizado(20L, 2L, "Secretaría", RolBase.ADMIN_IGLESIA,
			"miembros.ver");

	private final PoliticaRoles politica = new PoliticaRoles();

	@Test
	void losRolesBaseLosVenTodosYLosPersonalizadosSoloSuIglesia() {
		assertThatCode(() -> politica.verificarVisible(ADMIN_IGLESIA_1, RolesPrueba.base(RolBase.LIDER)))
			.doesNotThrowAnyException();
		assertThatCode(() -> politica.verificarVisible(ADMIN_IGLESIA_1, SECRETARIA_1)).doesNotThrowAnyException();
		assertThatThrownBy(() -> politica.verificarVisible(ADMIN_IGLESIA_1, SECRETARIA_2))
			.isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> politica.verificarVisible(ADMIN_SISTEMA, SECRETARIA_1))
			.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void losRolesSeCreanEnLaIglesiaDeQuienActuaYSinIglesiaNoSePuede() {
		assertThat(politica.iglesiaParaCrear(ADMIN_IGLESIA_1)).isEqualTo(1L);
		assertThatThrownBy(() -> politica.iglesiaParaCrear(ADMIN_SISTEMA)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void soloSeHeredaDeUnRolBaseQueNoSeaAdministradorDelSistema() {
		assertThatCode(() -> politica.verificarHeredable(RolesPrueba.base(RolBase.ADMIN_IGLESIA)))
			.doesNotThrowAnyException();
		assertThatCode(() -> politica.verificarHeredable(RolesPrueba.base(RolBase.MIEMBRO)))
			.doesNotThrowAnyException();
		assertThatThrownBy(() -> politica.verificarHeredable(RolesPrueba.base(RolBase.ADMIN_SISTEMA)))
			.isInstanceOf(ReglaNegocioException.class);
		assertThatThrownBy(() -> politica.verificarHeredable(SECRETARIA_1)).isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void sePuedeAsignarCualquierPermisoDeLosRolesBaseHeredablesPeroNoLosDelSistema() {
		List<Rol> heredables = List.of(RolesPrueba.base(RolBase.ADMIN_IGLESIA, "miembros.ver", "aportes.ver", "aportes.registrar"),
				RolesPrueba.base(RolBase.LIDER, "miembros.ver", "asistencia.grupo"), RolesPrueba.base(RolBase.MIEMBRO));

		assertThatCode(() -> politica.verificarPermisosAsignables(Set.of("aportes.ver", "aportes.registrar",
				"asistencia.grupo"), heredables))
			.doesNotThrowAnyException();
		assertThatCode(() -> politica.verificarPermisosAsignables(Set.of(), heredables)).doesNotThrowAnyException();
		assertThatThrownBy(() -> politica.verificarPermisosAsignables(Set.of("miembros.ver", "iglesias.ver",
				"inventado.algo"), heredables))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("[iglesias.ver, inventado.algo]");
	}

	@Test
	void losRolesBaseNoSeModifican() {
		assertThatThrownBy(() -> politica.verificarEditable(RolesPrueba.base(RolBase.LIDER)))
			.isInstanceOf(AccessDeniedException.class);
		assertThatCode(() -> politica.verificarEditable(SECRETARIA_1)).doesNotThrowAnyException();
	}

	@Test
	void noSeDesactivaUnRolQueDejariaUsuariosSinRolesActivos() {
		assertThatThrownBy(() -> politica.verificarDesactivable(true)).isInstanceOf(ConflictoException.class);
		assertThatCode(() -> politica.verificarDesactivable(false)).doesNotThrowAnyException();
	}

}
