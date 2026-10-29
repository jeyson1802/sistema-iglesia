package com.iglesia.seguridad.usuario.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.auth.service.TokenRefrescoService;
import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.GarantiaAdministradorIglesia;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.FiltroUsuarios;
import com.iglesia.seguridad.usuario.dto.MiUsuarioResponse;
import com.iglesia.seguridad.usuario.dto.RestablecerContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.mapper.UsuarioMapper;
import com.iglesia.seguridad.usuario.model.Correo;
import com.iglesia.seguridad.usuario.repository.EspecificacionesUsuario;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

/** Administración de usuarios y gestión de la cuenta propia. */
@Service
public class UsuarioService {

	private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

	private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado";

	private static final String CORREO_EN_USO = "Ya existe un usuario con ese correo";

	private final UsuarioRepository usuarios;

	private final PoliticaAdministracionUsuarios politica;

	private final ProveedorUsuarioAutenticado proveedorUsuario;

	private final PasswordEncoder codificador;

	private final TokenRefrescoService tokensRefresco;

	private final UsuarioMapper mapper;

	private final RolService roles;

	private final GarantiaAdministradorIglesia garantiaAdministrador;

	UsuarioService(UsuarioRepository usuarios, PoliticaAdministracionUsuarios politica,
			ProveedorUsuarioAutenticado proveedorUsuario, PasswordEncoder codificador,
			TokenRefrescoService tokensRefresco, UsuarioMapper mapper, RolService roles,
			GarantiaAdministradorIglesia garantiaAdministrador) {
		this.usuarios = usuarios;
		this.politica = politica;
		this.proveedorUsuario = proveedorUsuario;
		this.codificador = codificador;
		this.tokensRefresco = tokensRefresco;
		this.mapper = mapper;
		this.roles = roles;
		this.garantiaAdministrador = garantiaAdministrador;
	}

	@Transactional
	public UsuarioResponse crear(CrearUsuarioRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		Set<Rol> rolesAsignados = roles.rolesAsignables(solicitud.rolIds(), solicitud.iglesiaId());
		politica.verificarPuedeAdministrar(actor, solicitud.iglesiaId(), rolesBase(rolesAsignados));
		String correo = Correo.normalizar(solicitud.correo());
		if (usuarios.existsByCorreo(correo)) {
			throw new ConflictoException(CORREO_EN_USO);
		}
		if (solicitud.miembroId() != null
				&& usuarios.existsByIglesiaIdAndMiembroId(solicitud.iglesiaId(), solicitud.miembroId())) {
			throw new ConflictoException("El miembro ya tiene un usuario");
		}
		Usuario usuario = Usuario.nuevo(correo, codificador.encode(solicitud.contrasena()), solicitud.iglesiaId(),
				solicitud.miembroId(), rolesAsignados, true);
		Usuario guardado = usuarios.save(usuario);
		LOG.info("Usuario creado id={} iglesiaId={} por usuarioId={}", guardado.getId(), guardado.getIglesiaId(),
				actor.usuarioId());
		return mapper.aRespuesta(guardado);
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Long iglesiaId, FiltroUsuarios filtro, Pageable paginacion) {
		Long iglesia = politica.iglesiaParaListado(proveedorUsuario.actual(), iglesiaId);
		Specification<Usuario> criterio = Specification.allOf(EspecificacionesUsuario.deIglesia(iglesia),
				EspecificacionesUsuario.correoContiene(filtro.correo()),
				EspecificacionesUsuario.conEstado(filtro.activo()), EspecificacionesUsuario.conRol(filtro.rolId()),
				EspecificacionesUsuario.deMiembro(filtro.miembroId()));
		return usuarios.findAll(criterio, paginacion).map(mapper::aRespuesta);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtener(Long id) {
		return mapper.aRespuesta(buscarAdministrable(proveedorUsuario.actual(), id));
	}

	@Transactional
	public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		if (actor.usuarioId().equals(id)) {
			throw new ReglaNegocioException("No puede modificar sus propios roles ni su estado");
		}
		Usuario usuario = buscarAdministrable(actor, id);
		Set<Rol> rolesAsignados = roles.rolesAsignables(solicitud.rolIds(), usuario.getIglesiaId());
		politica.verificarPuedeAdministrar(actor, usuario.getIglesiaId(), rolesBase(rolesAsignados));
		String correo = Correo.normalizar(solicitud.correo());
		if (usuarios.existsByCorreoAndIdNot(correo, id)) {
			throw new ConflictoException(CORREO_EN_USO);
		}
		if (solicitud.miembroId() != null
				&& usuarios.existsByIglesiaIdAndMiembroIdAndIdNot(usuario.getIglesiaId(), solicitud.miembroId(), id)) {
			throw new ConflictoException("El miembro ya tiene un usuario");
		}
		boolean cambioCorreo = usuario.cambiarCorreo(correo);
		boolean cambioAcceso = usuario.actualizar(rolesAsignados, solicitud.miembroId(), solicitud.activo());
		// El administrador del sistema no pertenece a ninguna iglesia.
		if (usuario.getIglesiaId() != null) {
			garantiaAdministrador.verificarQueQuedaAdministrador(usuario.getIglesiaId());
		}
		if (cambioCorreo || cambioAcceso) {
			// Fuerza a obtener un token nuevo con los datos actualizados (o impide el acceso si se desactivó o
			// cambió el correo con el que inicia sesión).
			tokensRefresco.revocarTodos(usuario.getId());
		}
		if (cambioCorreo) {
			LOG.info("Correo cambiado usuarioId={} por usuarioId={}; sesiones revocadas", usuario.getId(),
					actor.usuarioId());
		}
		LOG.info("Usuario actualizado id={} por usuarioId={}", usuario.getId(), actor.usuarioId());
		return mapper.aRespuesta(usuario);
	}

	@Transactional(readOnly = true)
	public MiUsuarioResponse obtenerActual() {
		return mapper.aMiUsuario(buscarActual(), proveedorUsuario.actual());
	}

	@Transactional
	public void cambiarContrasena(CambiarContrasenaRequest solicitud) {
		Usuario usuario = buscarActual();
		if (!codificador.matches(solicitud.contrasenaActual(), usuario.getHashContrasena())) {
			throw new ReglaNegocioException("La contraseña actual no es correcta");
		}
		usuario.cambiarContrasena(codificador.encode(solicitud.contrasenaNueva()));
		tokensRefresco.revocarTodos(usuario.getId());
		LOG.info("Contraseña cambiada usuarioId={}; sesiones revocadas", usuario.getId());
	}

	/**
	 * Fija una contraseña temporal a otro usuario: deberá cambiarla en su próximo inicio de sesión. Cierra sus
	 * sesiones para que no pueda seguir renovando un token emitido con la contraseña anterior.
	 */
	@Transactional
	public void restablecerContrasena(Long id, RestablecerContrasenaRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		if (actor.usuarioId().equals(id)) {
			throw new ReglaNegocioException("Para cambiar su propia contraseña use «Cambiar contraseña»");
		}
		Usuario usuario = buscarAdministrable(actor, id);
		usuario.restablecerContrasena(codificador.encode(solicitud.contrasenaTemporal()));
		tokensRefresco.revocarTodos(usuario.getId());
		LOG.info("Contraseña restablecida usuarioId={} por usuarioId={}; sesiones revocadas", usuario.getId(),
				actor.usuarioId());
	}

	/** Los usuarios fuera del alcance del actor se tratan como inexistentes para no revelar su existencia. */
	private Usuario buscarAdministrable(UsuarioAutenticado actor, Long id) {
		return usuarios.findConRolesById(id)
			.filter(usuario -> politica.puedeVer(actor, usuario))
			.orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
	}

	private static Set<RolBase> rolesBase(Set<Rol> roles) {
		return roles.stream().map(Rol::rolBaseEfectivo).collect(Collectors.toSet());
	}

	private Usuario buscarActual() {
		return usuarios.findConRolesById(proveedorUsuario.actual().usuarioId())
			.orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
	}

}
