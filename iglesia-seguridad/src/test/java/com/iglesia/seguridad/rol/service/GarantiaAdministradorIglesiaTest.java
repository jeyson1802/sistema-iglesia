package com.iglesia.seguridad.rol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.rol.repository.RolRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class GarantiaAdministradorIglesiaTest {

	private static final long IGLESIA = 7L;

	@Mock
	private RolRepository roles;

	@Test
	void sinAdministradoresActivosEsUnConflicto() {
		given(roles.contarAdministradoresActivos(IGLESIA, Permisos.USUARIOS_ADMINISTRAR)).willReturn(0L);

		assertThatThrownBy(() -> new GarantiaAdministradorIglesia(roles).verificarQueQuedaAdministrador(IGLESIA))
			.isInstanceOf(ConflictoException.class)
			.hasMessage(GarantiaAdministradorIglesia.SIN_ADMINISTRADOR);
	}

	@Test
	void conUnAdministradorActivoBasta() {
		given(roles.contarAdministradoresActivos(IGLESIA, Permisos.USUARIOS_ADMINISTRAR)).willReturn(1L);

		assertThatCode(() -> new GarantiaAdministradorIglesia(roles).verificarQueQuedaAdministrador(IGLESIA))
			.doesNotThrowAnyException();
	}

	@Test
	void escribeLosCambiosYBloqueaLaIglesiaAntesDeContar() {
		given(roles.contarAdministradoresActivos(IGLESIA, Permisos.USUARIOS_ADMINISTRAR)).willReturn(2L);

		new GarantiaAdministradorIglesia(roles).verificarQueQuedaAdministrador(IGLESIA);

		InOrder orden = inOrder(roles);
		orden.verify(roles).flush();
		orden.verify(roles).bloquearAdministracionDeIglesia(IGLESIA);
		orden.verify(roles).contarAdministradoresActivos(IGLESIA, Permisos.USUARIOS_ADMINISTRAR);
	}

}
