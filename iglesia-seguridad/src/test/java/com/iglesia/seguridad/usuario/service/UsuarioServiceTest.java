package com.iglesia.seguridad.usuario.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.entity.RolesPrueba;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.MiUsuarioResponse;
import com.iglesia.seguridad.usuario.dto.PerfilResponse;
import com.iglesia.seguridad.usuario.dto.RolAsignadoResponse;
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
			Set.of(RolBase.ADMIN_IGLESIA), Set.of());

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
		// Los roles base tienen los mismos ids que les asigna Flyway (1 a 4, en el orden del enum).
		lenient().when(roles.rolesAsignables(any(), any())).thenAnswer(invocacion -> invocacion.<Set<Long>>getArgument(0)
			.stream()
			.map(id -> RolesPrueba.base(RolBase.values()[(int) (id - 1)]))
			.collect(Collectors.toSet()));
	}

	private static Set<Long> rolIds(RolBase... rolesBase) {
		return Arrays.stream(rolesBase).map(rol -> RolesPrueba.base(rol).getId()).collect(Collectors.toSet());
	}

	@Test
	void creaUsuarioEnSuIglesiaConContrasenaCodificada() {
		given(codificador.encode("ClaveSegura123")).willReturn("{bcrypt}hash");
		given(usuarios.save(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));

		UsuarioResponse creado = servicio
			.crear(new CrearUsuarioRequest("Lider@Iglesia.com", "ClaveSegura123", 1L, 7L, rolIds(RolBase.LIDER)));

		assertThat(creado.correo()).isEqualTo("lider@iglesia.com");
		assertThat(creado.iglesiaId()).isEqualTo(1L);
		assertThat(creado.roles()).containsExactly(new RolAsignadoResponse(3L, "LIDER", RolBase.LIDER));
	}

	@Test
	void laContrasenaDeUnUsuarioCreadoPorUnAdministradorEsTemporal() {
		given(codificador.encode("ClaveSegura123")).willReturn("{bcrypt}hash");
		given(usuarios.save(any(Usuario.class))).willAnswer(invocacion -> invocacion.getArgument(0));

		servicio.crear(new CrearUsuarioRequest("a@b.com", "ClaveSegura123", 1L, null, rolIds(RolBase.ADMIN_IGLESIA)));

		ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
		then(usuarios).should().save(guardado.capture());
		assertThat(guardado.getValue().isDebeCambiarContrasena()).isTrue();
	}

	@Test
	void noCreaUsuariosEnOtraIglesia() {
		assertThatThrownBy(() -> servicio
			.crear(new CrearUsuarioRequest("a@b.com", "ClaveSegura123", 2L, null, rolIds(RolBase.ADMIN_IGLESIA))))
			.isInstanceOf(AccessDeniedException.class);
		then(usuarios).should(never()).save(any());
	}

	@Test
	void quienNoEsAdministradorDelSistemaNoLoOtorga() {
		assertThatThrownBy(() -> servicio.crear(new CrearUsuarioRequest("a@b.com", "ClaveSegura123", 1L, null,
				rolIds(RolBase.ADMIN_SISTEMA))))
			.isInstanceOf(AccessDeniedException.class);
		then(usuarios).should(never()).save(any());
	}

	@Test
	void validaLosRolesContraLaIglesiaDelUsuarioAlActualizar() {
		Usuario miembro = Usuario.nuevo("m@y.com", "h", 1L, 7L, RolesPrueba.bases(RolBase.MIEMBRO), false);
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(miembro));

		servicio.actualizar(50L, new ActualizarUsuarioRequest(rolIds(RolBase.LIDER), 7L, true));

		then(roles).should().rolesAsignables(rolIds(RolBase.LIDER), 1L);
	}

	@Test
	void miUsuarioDevuelveUnPerfilPorRolActivoConSusPermisos() {
		Rol secretaria = RolesPrueba.personalizado(10L, 1L, "Secretaría", RolBase.ADMIN_IGLESIA, "miembros.ver",
				"miembros.crear");
		Rol tesoreria = RolesPrueba
			.desactivado(RolesPrueba.personalizado(11L, 1L, "Tesorería", RolBase.ADMIN_IGLESIA, "usuarios.ver"));
		Usuario propio = Usuario.nuevo("s@y.com", "h", 1L, 7L,
				Set.of(RolesPrueba.base(RolBase.MIEMBRO), secretaria, tesoreria), false);
		given(usuarios.findConRolesById(ADMIN_IGLESIA_1.usuarioId())).willReturn(Optional.of(propio));

		MiUsuarioResponse yo = servicio.obtenerActual();

		assertThat(yo.perfiles()).containsExactly(
				new PerfilResponse(10L, "Secretaría", RolBase.ADMIN_IGLESIA, List.of("miembros.crear", "miembros.ver")),
				new PerfilResponse(4L, "MIEMBRO", RolBase.MIEMBRO, List.of()));
	}

	@Test
	void rechazaCorreoDuplicado() {
		given(usuarios.existsByCorreo("a@b.com")).willReturn(true);

		assertThatThrownBy(() -> servicio
			.crear(new CrearUsuarioRequest("A@b.com", "ClaveSegura123", 1L, null, rolIds(RolBase.ADMIN_IGLESIA))))
			.isInstanceOf(ConflictoException.class);
	}

	@Test
	void unUsuarioDeOtraIglesiaSeTrataComoInexistente() {
		Usuario ajeno = Usuario.nuevo("x@y.com", "h", 2L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(ajeno));

		assertThatThrownBy(() -> servicio.obtener(50L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void revocaSesionesCuandoCambianLosRoles() {
		Usuario miembro = Usuario.nuevo("m@y.com", "h", 1L, 7L, RolesPrueba.bases(RolBase.MIEMBRO), false);
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(miembro));

		servicio.actualizar(50L, new ActualizarUsuarioRequest(rolIds(RolBase.LIDER), 7L, true));

		then(tokensRefresco).should().revocarTodos(miembro.getId());
	}

	@Test
	void noRevocaSesionesSiNoCambiaElAcceso() {
		Usuario miembro = Usuario.nuevo("m@y.com", "h", 1L, 7L, RolesPrueba.bases(RolBase.MIEMBRO), false);
		given(usuarios.findConRolesById(50L)).willReturn(Optional.of(miembro));

		servicio.actualizar(50L, new ActualizarUsuarioRequest(rolIds(RolBase.MIEMBRO), 7L, true));

		then(tokensRefresco).should(never()).revocarTodos(anyLong());
	}

	@Test
	void nadiePuedeModificarSuPropioUsuario() {
		assertThatThrownBy(() -> servicio.actualizar(ADMIN_IGLESIA_1.usuarioId(),
				new ActualizarUsuarioRequest(rolIds(RolBase.ADMIN_IGLESIA), null, false)))
			.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void cambiarContrasenaExigeLaActualYRevocaSesiones() {
		Usuario propio = Usuario.nuevo("p@y.com", "hash-actual", 1L, null, RolesPrueba.bases(RolBase.ADMIN_IGLESIA), false);
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
