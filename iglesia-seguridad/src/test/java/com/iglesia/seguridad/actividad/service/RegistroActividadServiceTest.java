package com.iglesia.seguridad.actividad.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegistroActividadServiceTest {

	private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00Z");

	private final ActividadUsuarioRepository actividades = mock(ActividadUsuarioRepository.class);

	private final PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);

	private final RegistroActividadService servicio = new RegistroActividadService(actividades,
			Clock.fixed(AHORA, ZoneId.systemDefault()), transacciones);

	@Test
	void registraElDiaConElRolDeMayorAlcance() {
		given(transacciones.getTransaction(any())).willReturn(new SimpleTransactionStatus());
		Usuario usuario = usuario(Set.of(RolBase.MIEMBRO, RolBase.LIDER), null);

		servicio.registrar(usuario);

		verify(actividades).registrar(eq(9L), eq(LocalDate.ofInstant(AHORA, ZoneId.systemDefault())), isNull(),
				eq("LIDER"));
	}

	@Test
	void unaFallaAlRegistrarNoImpideEntrar() {
		given(transacciones.getTransaction(any())).willReturn(new SimpleTransactionStatus());
		willThrow(new DataIntegrityViolationException("sin conexión")).given(actividades)
			.registrar(anyLong(), any(), any(), anyString());

		assertThatCode(() -> servicio.registrar(usuario(Set.of(RolBase.ADMIN_IGLESIA), 4L))).doesNotThrowAnyException();
	}

	@Test
	void sinRolesActivosCuentaComoMiembro() {
		assertThat(RegistroActividadService.rolPrincipal(usuario(Set.of(), 4L))).isEqualTo(RolBase.MIEMBRO);
	}

	private static Usuario usuario(Set<RolBase> roles, Long iglesiaId) {
		Usuario usuario = mock(Usuario.class);
		given(usuario.getId()).willReturn(9L);
		given(usuario.getIglesiaId()).willReturn(iglesiaId);
		given(usuario.getRolesBaseActivos()).willReturn(roles);
		return usuario;
	}

}
