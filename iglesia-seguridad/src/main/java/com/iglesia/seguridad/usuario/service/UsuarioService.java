package com.iglesia.seguridad.usuario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.auth.service.TokenRefrescoService;
import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.mapper.UsuarioMapper;
import com.iglesia.seguridad.usuario.model.Correo;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

/** Administración de usuarios y gestión de la cuenta propia. */
@Service
public class UsuarioService {

	private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

	private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado";

	private final UsuarioRepository usuarios;

	private final PoliticaAdministracionUsuarios politica;

	private final ProveedorUsuarioAutenticado proveedorUsuario;

	private final PasswordEncoder codificador;

	private final TokenRefrescoService tokensRefresco;

	private final UsuarioMapper mapper;

	UsuarioService(UsuarioRepository usuarios, PoliticaAdministracionUsuarios politica,
			ProveedorUsuarioAutenticado proveedorUsuario, PasswordEncoder codificador,
			TokenRefrescoService tokensRefresco, UsuarioMapper mapper) {
		this.usuarios = usuarios;
		this.politica = politica;
		this.proveedorUsuario = proveedorUsuario;
		this.codificador = codificador;
		this.tokensRefresco = tokensRefresco;
		this.mapper = mapper;
	}

	@Transactional
	public UsuarioResponse crear(CrearUsuarioRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		politica.verificarPuedeAdministrar(actor, solicitud.iglesiaId(), solicitud.roles());
		String correo = Correo.normalizar(solicitud.correo());
		if (usuarios.existsByCorreo(correo)) {
			throw new ConflictoException("Ya existe un usuario con ese correo");
		}
		if (solicitud.miembroId() != null
				&& usuarios.existsByIglesiaIdAndMiembroId(solicitud.iglesiaId(), solicitud.miembroId())) {
			throw new ConflictoException("El miembro ya tiene un usuario");
		}
		Usuario usuario = Usuario.nuevo(correo, codificador.encode(solicitud.contrasena()), solicitud.iglesiaId(),
				solicitud.miembroId(), solicitud.roles());
		Usuario guardado = usuarios.save(usuario);
		LOG.info("Usuario creado id={} iglesiaId={} por usuarioId={}", guardado.getId(), guardado.getIglesiaId(),
				actor.usuarioId());
		return mapper.aRespuesta(guardado);
	}

	@Transactional(readOnly = true)
	public Page<UsuarioResponse> listar(Long iglesiaId, Pageable paginacion) {
		Long iglesia = politica.iglesiaParaListado(proveedorUsuario.actual(), iglesiaId);
		Page<Usuario> pagina = iglesia == null ? usuarios.findAll(paginacion)
				: usuarios.findByIglesiaId(iglesia, paginacion);
		return pagina.map(mapper::aRespuesta);
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
		politica.verificarPuedeAdministrar(actor, usuario.getIglesiaId(), solicitud.roles());
		if (solicitud.miembroId() != null
				&& usuarios.existsByIglesiaIdAndMiembroIdAndIdNot(usuario.getIglesiaId(), solicitud.miembroId(), id)) {
			throw new ConflictoException("El miembro ya tiene un usuario");
		}
		if (usuario.actualizar(solicitud.roles(), solicitud.miembroId(), solicitud.activo())) {
			// Fuerza a obtener un token nuevo con los datos actualizados (o impide el acceso si se desactivó).
			tokensRefresco.revocarTodos(usuario.getId());
		}
		LOG.info("Usuario actualizado id={} por usuarioId={}", usuario.getId(), actor.usuarioId());
		return mapper.aRespuesta(usuario);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtenerActual() {
		return mapper.aRespuesta(buscarActual());
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

	/** Los usuarios fuera del alcance del actor se tratan como inexistentes para no revelar su existencia. */
	private Usuario buscarAdministrable(UsuarioAutenticado actor, Long id) {
		return usuarios.findConRolesById(id)
			.filter(usuario -> politica.puedeVer(actor, usuario))
			.orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
	}

	private Usuario buscarActual() {
		return usuarios.findConRolesById(proveedorUsuario.actual().usuarioId())
			.orElseThrow(() -> new RecursoNoEncontradoException(USUARIO_NO_ENCONTRADO));
	}

}
