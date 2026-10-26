package com.iglesia.seguridad.rol.entity;

import org.junit.jupiter.api.Test;

import com.iglesia.seguridad.rol.model.RolBase;

import static org.assertj.core.api.Assertions.assertThat;

class RolTest {

	@Test
	void unRolBaseEsSuPropioRolBaseEfectivo() {
		Rol lider = RolesPrueba.base(RolBase.LIDER);

		assertThat(lider.esBase()).isTrue();
		assertThat(lider.rolBaseEfectivo()).isEqualTo(RolBase.LIDER);
	}

	@Test
	void unRolPersonalizadoTomaElRolBaseDelQueHereda() {
		Rol secretaria = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver");

		assertThat(secretaria.esBase()).isFalse();
		assertThat(secretaria.rolBaseEfectivo()).isEqualTo(RolBase.ADMIN_IGLESIA);
	}

	@Test
	void dosRolesSonIgualesSiTienenElMismoId() {
		assertThat(RolesPrueba.base(RolBase.MIEMBRO)).isEqualTo(RolesPrueba.base(RolBase.MIEMBRO))
			.isNotEqualTo(RolesPrueba.base(RolBase.LIDER));
	}

}
