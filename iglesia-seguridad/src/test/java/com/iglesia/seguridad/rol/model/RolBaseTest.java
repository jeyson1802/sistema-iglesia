package com.iglesia.seguridad.rol.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolBaseTest {

	@Test
	void cadaRolBaseTieneSuAmbito() {
		assertThat(RolBase.ADMIN_SISTEMA.ambito()).isEqualTo(Ambito.SISTEMA);
		assertThat(RolBase.ADMIN_IGLESIA.ambito()).isEqualTo(Ambito.IGLESIA);
		assertThat(RolBase.LIDER.ambito()).isEqualTo(Ambito.RED);
		assertThat(RolBase.MIEMBRO.ambito()).isEqualTo(Ambito.PROPIO);
	}

	@Test
	void soloLiderYMiembroRequierenMiembroVinculado() {
		assertThat(RolBase.values()).filteredOn(RolBase::requiereMiembro)
			.containsExactlyInAnyOrder(RolBase.LIDER, RolBase.MIEMBRO);
	}

	@Test
	void todosSonHeredablesMenosElAdministradorDelSistema() {
		assertThat(RolBase.values()).filteredOn(rol -> !rol.heredable()).containsExactly(RolBase.ADMIN_SISTEMA);
	}

}
