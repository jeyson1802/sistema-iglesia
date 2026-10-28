package com.iglesia.seguridad.rol.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.dto.ActualizarRolRequest;
import com.iglesia.seguridad.rol.dto.CambiarEstadoRolRequest;
import com.iglesia.seguridad.rol.dto.CrearRolRequest;
import com.iglesia.seguridad.rol.dto.PermisoResponse;
import com.iglesia.seguridad.rol.dto.RolResponse;
import com.iglesia.seguridad.rol.entity.Permiso;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.mapper.RolMapper;
import com.iglesia.seguridad.rol.model.CantidadUsuariosRol;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.repository.PermisoRepository;
import com.iglesia.seguridad.rol.repository.RolRepository;

/** Administración de los roles personalizados de la iglesia y consulta de roles para los demás contextos. */
@Service
public class RolService {

	private static final Logger LOG = LoggerFactory.getLogger(RolService.class);

	/** Primero los roles base y luego los personalizados, cada grupo por jerarquía del rol base y por nombre. */
	private static final Comparator<Rol> ORDEN_LISTADO = Comparator.comparing((Rol rol) -> !rol.esBase())
		.thenComparing(Rol::rolBaseEfectivo)
		.thenComparing(Rol::getNombre, String.CASE_INSENSITIVE_ORDER);

	private static final String NOMBRE_REPETIDO = "Ya existe un rol con ese nombre en la iglesia";

	private final RolRepository roles;

	private final PermisoRepository permisos;

	private final PoliticaRoles politica;

	private final ProveedorUsuarioAutenticado proveedorUsuario;

	private final RolMapper mapper;

	private final GarantiaAdministradorIglesia garantiaAdministrador;

	RolService(RolRepository roles, PermisoRepository permisos, PoliticaRoles politica,
			ProveedorUsuarioAutenticado proveedorUsuario, RolMapper mapper,
			GarantiaAdministradorIglesia garantiaAdministrador) {
		this.roles = roles;
		this.permisos = permisos;
		this.politica = politica;
		this.proveedorUsuario = proveedorUsuario;
		this.mapper = mapper;
		this.garantiaAdministrador = garantiaAdministrador;
	}

	@Transactional(readOnly = true)
	public List<PermisoResponse> listarPermisos() {
		return permisos.findAllByOrderByModuloAscOrdenAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional(readOnly = true)
	public List<RolResponse> listar() {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		List<Rol> visibles = roles.findByIglesiaIdIsNullOrIglesiaId(actor.iglesiaId());
		Map<Long, Long> cantidades = contarUsuarios(visibles.stream().map(Rol::getId).toList(), actor);
		return visibles.stream()
			.sorted(ORDEN_LISTADO)
			.map(rol -> mapper.aRespuesta(rol, cantidades.getOrDefault(rol.getId(), 0L)))
			.toList();
	}

	@Transactional(readOnly = true)
	public RolResponse obtener(Long id) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		return aRespuesta(buscarVisible(actor, id), actor);
	}

	@Transactional
	public RolResponse crear(CrearRolRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		Long iglesiaId = politica.iglesiaParaCrear(actor);
		Rol rolBase = roles.findConPermisosById(solicitud.rolBaseId())
			.orElseThrow(() -> new ReglaNegocioException("El rol base indicado no existe"));
		politica.verificarHeredable(rolBase);
		politica.verificarPermisosDelRolBase(solicitud.permisos(), rolBase);
		if (roles.existsByIglesiaIdAndNombreIgnoreCase(iglesiaId, solicitud.nombre().strip())) {
			throw new ConflictoException(NOMBRE_REPETIDO);
		}
		Rol rol = roles
			.save(Rol.personalizado(iglesiaId, solicitud.nombre(), rolBase, buscarPermisos(solicitud.permisos())));
		LOG.info("Rol creado id={} iglesiaId={} por usuarioId={}", rol.getId(), iglesiaId, actor.usuarioId());
		return mapper.aRespuesta(rol, 0);
	}

	@Transactional
	public RolResponse actualizar(Long id, ActualizarRolRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		Rol rol = buscarVisible(actor, id);
		politica.verificarEditable(rol);
		politica.verificarPermisosDelRolBase(solicitud.permisos(), rol.getRolBase());
		if (roles.existsByIglesiaIdAndNombreIgnoreCaseAndIdNot(rol.getIglesiaId(), solicitud.nombre().strip(), id)) {
			throw new ConflictoException(NOMBRE_REPETIDO);
		}
		rol.actualizar(solicitud.nombre(), buscarPermisos(solicitud.permisos()));
		garantiaAdministrador.verificarQueQuedaAdministrador(rol.getIglesiaId());
		LOG.info("Rol actualizado id={} iglesiaId={} por usuarioId={}", id, rol.getIglesiaId(), actor.usuarioId());
		return aRespuesta(rol, actor);
	}

	@Transactional
	public RolResponse cambiarEstado(Long id, CambiarEstadoRolRequest solicitud) {
		UsuarioAutenticado actor = proveedorUsuario.actual();
		Rol rol = buscarVisible(actor, id);
		politica.verificarEditable(rol);
		boolean activar = solicitud.activo();
		if (!activar) {
			politica.verificarDesactivable(roles.dejariaUsuariosSinRolesActivos(id));
		}
		boolean cambio = rol.cambiarEstado(activar);
		garantiaAdministrador.verificarQueQuedaAdministrador(rol.getIglesiaId());
		if (cambio) {
			LOG.info("Rol {} id={} iglesiaId={} por usuarioId={}", activar ? "reactivado" : "desactivado", id,
					rol.getIglesiaId(), actor.usuarioId());
		}
		return aRespuesta(rol, actor);
	}

	/** Roles base con esos códigos. Flyway siembra los 4, así que nunca falta ninguno. */
	@Transactional(readOnly = true)
	public Set<Rol> rolesBase(Set<RolBase> codigos) {
		Set<Rol> encontrados = new HashSet<>(roles.findByCodigoIn(codigos));
		if (encontrados.size() != codigos.size()) {
			throw new IllegalStateException("Faltan roles base sembrados por Flyway: " + codigos);
		}
		return encontrados;
	}

	/**
	 * Roles que se pueden asignar a un usuario de esa iglesia: roles base o roles personalizados activos de la
	 * misma iglesia. Un rol de otra iglesia se trata como inexistente para no revelar su existencia.
	 * @throws ReglaNegocioException si algún rol no existe, es de otra iglesia o está inactivo
	 */
	@Transactional(readOnly = true)
	public Set<Rol> rolesAsignables(Set<Long> ids, Long iglesiaId) {
		Set<Rol> encontrados = new HashSet<>(roles.findByIdIn(ids));
		if (encontrados.size() != ids.size()
				|| encontrados.stream().anyMatch(rol -> !rol.esBase() && !rol.getIglesiaId().equals(iglesiaId))) {
			throw new ReglaNegocioException("Algún rol no existe o no pertenece a la iglesia del usuario");
		}
		if (encontrados.stream().anyMatch(rol -> !rol.isActivo())) {
			throw new ReglaNegocioException("No se puede asignar un rol inactivo");
		}
		return encontrados;
	}

	private Rol buscarVisible(UsuarioAutenticado actor, Long id) {
		Rol rol = roles.findConPermisosById(id)
			.orElseThrow(() -> new RecursoNoEncontradoException(PoliticaRoles.ROL_NO_ENCONTRADO));
		politica.verificarVisible(actor, rol);
		return rol;
	}

	private Set<Permiso> buscarPermisos(Set<String> codigos) {
		return new HashSet<>(permisos.findAllById(codigos));
	}

	private RolResponse aRespuesta(Rol rol, UsuarioAutenticado actor) {
		return mapper.aRespuesta(rol, contarUsuarios(List.of(rol.getId()), actor).getOrDefault(rol.getId(), 0L));
	}

	/** Cuenta solo los usuarios de la iglesia de quien consulta, para no revelar datos de otras iglesias. */
	private Map<Long, Long> contarUsuarios(Collection<Long> rolIds, UsuarioAutenticado actor) {
		if (rolIds.isEmpty()) {
			return Map.of();
		}
		return roles.contarUsuarios(rolIds, actor.iglesiaId())
			.stream()
			.collect(Collectors.toMap(CantidadUsuariosRol::getRolId, CantidadUsuariosRol::getCantidad));
	}

}
