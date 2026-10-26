package com.iglesia.seguridad.usuario.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iglesia.seguridad.auth.service.TokenRefrescoService;
import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.mapper.UsuarioMapper;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	private static final UsuarioAutenticado ADMIN_IGLESIA_1 = new UsuarioAutenticado(2L, 1L, null,
			Set.of(RolBase.ADMIN_IGLESIA));

	@Mock
	private UsuarioRepository usuarios;

	@Mock
	private ProveedorUsuarioAutenticado proveedorUsuario;

	@Mock
	private PasswordEncoder codificador;

	@Mock
	private TokenRefrescoService tokensRefresco;

	@Mock
	private RolService roles;

	private UsuarioService servicio;

	@BeforeEach
	void configurar() {
		servicio = new UsuarioService(usuarios, new PoliticaAdministracionUsuarios(), proveedorUsuario, codificador,
				tokensRefresco, new UsuarioMapper(), roles);
		given(proveedorUsuario.actual()).willReturn(ADMIN_IGLESIA_1);
		lenient().when(roles.rolesBase(any())).thenAnswer(
				invocacion -> RolesPrueba.bases(invocacion.<Set<RolBase>>getArgument(0).toArray(RolBase[]::new)));
	}

	@Test
	void creaUsuarioEnSuIglesiaConContrasenaCodificada() {
		given(codificador.encode("ClaveSegura123")).willReturn("{bcrypt}hash");
		given(usuarios.save(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));

		UsuarioResponse creado = servicio
			.crear(new CrearUsuarioRequest("Lider@Iglesia.com", "ClaveSegura123", 1L, 7L, Set.of(RolBase.LIDER)));

		assertThat(creado.correo()).isEqualTo("lider@iglesia.com");
		assertThat(creado.iglesiaId()).isEqualTo(1L);
		assertThat(creado.roles()).containsExactly(RolBase.LIDER);
	}

	@Test
	void noCreaUsuariosEnOtraIglesia() {
		assertThatThrownBy(() -> servicio
			.crear(new CrearUsuarioRequest("a@b.com", "ClaveSegura123", 2L, null, Set.of(RolBase.ADMIN_IGLESIA))))
			.isInstanceOf(AccessDeniedException.class);
		then(usuarios).should(never()).save(any());
	}

	@Test
	void rechazaCorreoDuplicado() {
		given(usuarios.existsByCorreo("a@b.com")).willReturn(true);

		assertThatThrownBy(() -> servicio
			.crear(new CrearUsuarioRequest("A@b.com", "ClaveSegura123", 1L, null, Set.of(RolBase.ADMIN_IGLESIA))))
			.isInstanceOf(ConflictoException.class);
	}

	@Test
	void unUsuarioDeOtraIglesiaSeTrataComoInexistente() {
		Usuario ajeno = Usuario.nuevo("x@y.com", "h", 2L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA));
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(ajeno));

		assertThatThrownBy(() -> servicio.obtener(50L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void revocaSesionesCuandoCambianLosRoles() {
		Usuario miembro = Usuario.nuevo("m@y.com", "h", 1L, 7L, RolesPrueba.bases(RolBase.MIEMBRO));
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(miembro));

		servicio.actualizar(50L, new ActualizarUsuarioRequest(Set.of(RolBase.LIDER), 7L, true));

		then(tokensRefresco).should().revocarTodos(miembro.getId());
	}

	@Test
	void noRevocaSesionesSiNoCambiaElAcceso() {
		Usuario miembro = Usuario.nuevo("m@y.com", "h", 1L, 7L, RolesPrueba.bases(RolBase.MIEMBRO));
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(miembro));

		servicio.actualizar(50L, new ActualizarUsuarioRequest(Set.of(RolBase.MIEMBRO), 7L, true));

		then(tokensRefresco).should(never()).revocarTodos(anyLong());
	}

	@Test
	void nadiePuedeModificarSuPropioUsuario() {
		assertThatThrownBy(() -> servicio.actualizar(ADMIN_IGLESIA_1.usuarioId(),
				new ActualizarUsuarioRequest(Set.of(RolBase.ADMIN_IGLESIA), null, false)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void cambiarContrasenaExigeLaActualYRevocaSesiones() {
		Usuario propio = Usuario.nuevo("p@y.com", "hash-actual", 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA));
		given(usuarios.findConRolesById(ADMIN_IGLESIA_1.usuarioId())).willReturn(Optional.of(propio));
		given(codificador.matches("mala", "hash-actual")).willReturn(false);
		given(codificador.matches("buena", "hash-actual")).willReturn(true);
		given(codificador.encode("NuevaClave123")).willReturn("hash-nuevo");

		assertThatThrownBy(() -> servicio.cambiarContrasena(new CambiarContrasenaRequest("mala", "NuevaClave123")))
			.isInstanceOf(ReglaNegocioException.class);

		servicio.cambiarContrasena(new CambiarContrasenaRequest("buena", "NuevaClave123"));

		assertThat(propio.getHashContrasena()).isEqualTo("hash-nuevo");
		then(tokensRefresco).should().revocarTodos(propio.getId());
	}

}
