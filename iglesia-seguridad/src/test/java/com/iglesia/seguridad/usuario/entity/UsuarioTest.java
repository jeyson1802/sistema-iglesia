package com.iglesia.seguridad.usuario.entity;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.usuario.model.Rol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

	private static final String HASH = "{noop}hash";

	@Test
	void normalizaElCorreoYQuedaActivo() {
		Usuario usuario = Usuario.nuevo("  Pastor@Iglesia.COM ", HASH, 1L, null, Set.of(Rol.ADMIN_IGLESIA));

		assertThat(usuario.getCorreo()).isEqualTo("pastor@iglesia.com");
		assertThat(usuario.isActivo()).isTrue();
	}

	@Test
	void elAdministradorDelSistemaNoPerteneceAUnaIglesia() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, Set.of(Rol.ADMIN_SISTEMA)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void losDemasRolesRequierenIglesia() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, null, null, Set.of(Rol.ADMIN_IGLESIA)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void liderYMiembroRequierenMiembroVinculado() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, Set.of(Rol.LIDER)))
			.isInstanceOf(ReglaNegocioException.class);
		assertThat(Usuario.nuevo("a@b.com", HASH, 1L, 10L, Set.of(Rol.MIEMBRO)).getMiembroId()).isEqualTo(10L);
	}

	@Test
	void requiereAlMenosUnRol() {
		assertThatThrownBy(() -> Usuario.nuevo("a@b.com", HASH, 1L, null, Set.of()))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void actualizarIndicaSiCambioElAcceso() {
		Usuario usuario = Usuario.nuevo("a@b.com", HASH, 1L, 10L, Set.of(Rol.MIEMBRO));

		assertThat(usuario.actualizar(Set.of(Rol.MIEMBRO), 10L, true)).isFalse();
		assertThat(usuario.actualizar(Set.of(Rol.LIDER), 10L, true)).isTrue();
		assertThat(usuario.actualizar(Set.of(Rol.LIDER), 10L, false)).isTrue();
		assertThat(usuario.isActivo()).isFalse();
		assertThat(usuario.getRoles()).containsExactly(Rol.LIDER);
	}

}
