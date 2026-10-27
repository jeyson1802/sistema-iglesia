package com.iglesia.seguridad.rol.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.mapper.RolMapper;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.repository.PermisoRepository;
import com.iglesia.seguridad.rol.repository.RolRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

	private static final long IGLESIA = 1L;

	@Mock
	private RolRepository repositorio;

	@Mock
	private PermisoRepository permisos;

	@Mock
	private ProveedorUsuarioAutenticado proveedorUsuario;

	private RolService servicio() {
		return new RolService(repositorio, permisos, new PoliticaRoles(), proveedorUsuario, new RolMapper());
	}

	@Test
	void asignaRolesBaseYPersonalizadosActivosDeLaMismaIglesia() {
		Rol lider = RolesPrueba.base(RolBase.LIDER);
		Rol secretaria = RolesPrueba.personalizado(10L, IGLESIA, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver");
		given(repositorio.findByIdIn(Set.of(3L, 10L))).willReturn(List.of(lider, secretaria));

		assertThat(servicio().rolesAsignables(Set.of(3L, 10L), IGLESIA)).containsExactlyInAnyOrder(lider, secretaria);
	}

	@Test
	void unRolInexistenteEsUnErrorDeDatos() {
		given(repositorio.findByIdIn(Set.of(3L, 99L))).willReturn(List.of(RolesPrueba.base(RolBase.LIDER)));

		assertThatThrownBy(() -> servicio().rolesAsignables(Set.of(3L, 99L), IGLESIA))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("no existe");
	}

	@Test
	void unRolDeOtraIglesiaSeTrataComoInexistente() {
		Rol ajeno = RolesPrueba.personalizado(20L, 2L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver");
		given(repositorio.findByIdIn(Set.of(20L))).willReturn(List.of(ajeno));

		assertThatThrownBy(() -> servicio().rolesAsignables(Set.of(20L), IGLESIA))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("no existe");
	}

	@Test
	void unRolInactivoNoSeAsigna() {
		Rol inactivo = RolesPrueba
			.desactivado(RolesPrueba.personalizado(10L, IGLESIA, "Tesorería", RolBase.ADMIN_IGLESIA, "usuarios.ver"));
		given(repositorio.findByIdIn(Set.of(10L))).willReturn(List.of(inactivo));

		assertThatThrownBy(() -> servicio().rolesAsignables(Set.of(10L), IGLESIA))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("inactivo");
	}

}
