package com.iglesia.seguridad.usuario.entity;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

	private static final String HASH = "{noop}hash";

	@Test
	void normalizaElCorreoYQuedaActivo() {
		Usuario usuario = Usuario.nuevo("  Pastor@Iglesia.COM ", HASH, 1L, null,
				RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);

		assertThat(usuario.getCorreo()).isEqualTo("pastor@iglesia.com");
		assertThat(usuario.isActivo()).isTrue();
	}

	@Test
	void elAdministradorDelSistemaNoPerteneceAUnaIglesia() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.ADMIN_SISTEMA), false))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void losDemasRolesRequierenIglesia() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, null, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void liderYMiembroRequierenMiembroVinculado() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.LIDER), false))
			.isInstanceOf(ReglaNegocioException.class);
		assertThat(Usuario.nuevo("a@b.com", HASH, 1L, 10L, RolesPrueba.bases(RolBase.MIEMBRO), false).getMiembroId())
			.isEqualTo(10L);
	}

	@Test
	void unRolPersonalizadoQueHeredaDeLiderRequiereMiembroVinculado() {
		Rol coordinador = RolesPrueba.personalizado(10L, 1L, "Coordinador", RolBase.LIDER);

		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, Set.of(coordinador), false))
			.isInstanceOf(ReglaNegocioException.class);
		assertThat(Usuario.nuevo("a@b.com", HASH, 1L, 10L, Set.of(coordinador), false).getRolesBase())
			.containsExactly(RolBase.LIDER);
	}

	@Test
	void unRolPersonalizadoQueHeredaDeAdminIglesiaRequiereIglesia() {
		Rol secretaria = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver");

		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, null, null, Set.of(secretaria), false))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void losRolesBaseSeDerivanSinRepetir() {
		Rol secretaria = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver");
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, 10L,
				Set.of(secretaria, RolesPrueba.base(RolBase.ADMIN_IGLESIA), RolesPrueba.base(RolBase.MIEMBRO)), false);

		assertThat(usuario.getRolesBase()).containsExactly(RolBase.ADMIN_IGLESIA, RolBase.MIEMBRO);
	}

	@Test
	void unRolDesactivadoNoAportaPermisosNiRolBase() {
		Rol secretaria = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver",
				"miembros.crear");
		Rol tesoreria = RolesPrueba
			.desactivado(RolesPrueba.personalizado(11L, 1L, "Tesorería", RolBase.LIDER, "usuarios.ver"));
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, 10L, Set.of(secretaria, tesoreria), false);

		assertThat(usuario.getRolesBaseActivos()).containsExactly(RolBase.ADMIN_IGLESIA);
		assertThat(usuario.getPermisosActivos()).containsExactlyInAnyOrder("miembros.ver", "miembros.crear");
		assertThat(usuario.getRolesBase()).containsExactly(RolBase.ADMIN_IGLESIA, RolBase.LIDER);
	}

	@Test
	void requiereAlMenosUnRol() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, Set.of(), false))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void actualizarIndicaSiCambioElAcceso() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, 10L, RolesPrueba.bases(RolBase.MIEMBRO), false);

		assertThat(usuario.actualizar(RolesPrueba.bases(RolBase.MIEMBRO), 10L, true)).isFalse();
		assertThat(usuario.actualizar(RolesPrueba.bases(RolBase.LIDER), 10L, true)).isTrue();
		assertThat(usuario.actualizar(RolesPrueba.bases(RolBase.LIDER), 10L, false)).isTrue();
		assertThat(usuario.isActivo()).isFalse();
		assertThat(usuario.getRoles()).containsExactly(RolesPrueba.base(RolBase.LIDER));
	}

	@Test
	void unaContrasenaElegidaPorUnAdministradorEsTemporal() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), true);

		assertThat(usuario.isDebeCambiarContrasena()).isTrue();
	}

	@Test
	void unaContrasenaNoTemporalNoObligaACambiarla() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);

		assertThat(usuario.isDebeCambiarContrasena()).isFalse();
	}

	@Test
	void cambiarLaPropiaContrasenaDejaDeSerTemporal() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), true);

		usuario.cambiarContrasena("{noop}nuevo");

		assertThat(usuario.getHashContrasena()).isEqualTo("{noop}nuevo");
		assertThat(usuario.isDebeCambiarContrasena()).isFalse();
	}

	@Test
	void restablecerLaContrasenaLaVuelveTemporal() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);

		usuario.restablecerContrasena("{noop}temporal");

		assertThat(usuario.getHashContrasena()).isEqualTo("{noop}temporal");
		assertThat(usuario.isDebeCambiarContrasena()).isTrue();
	}

}
