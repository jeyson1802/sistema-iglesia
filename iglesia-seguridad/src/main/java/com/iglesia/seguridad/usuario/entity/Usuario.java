package com.iglesia.seguridad.usuario.entity;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.rol.entity.Permiso;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.model.Correo;

/**
 * Cuenta de acceso. Protege sus invariantes según el rol base efectivo de sus roles: el administrador
 * del sistema no pertenece a ninguna iglesia, el resto sí, y los roles de líder y miembro (y los que
 * heredan de ellos) exigen un miembro vinculado.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 254)
	private String correo;

	@Column(name = "hash_contrasena", nullable = false)
	private String hashContrasena;

	@Column(name = "iglesia_id", updatable = false)
	private Long iglesiaId;

	@Column(name = "miembro_id")
	private Long miembroId;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "debe_cambiar_contrasena", nullable = false)
	private boolean debeCambiarContrasena;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"),
			inverseJoinColumns = @JoinColumn(name = "rol_id"))
	private Set<Rol> roles = new HashSet<>();

	@CreationTimestamp
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@UpdateTimestamp
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@Version
	private long version;

	protected Usuario() {
	}

	private Usuario(String correo, String hashContrasena, Long iglesiaId, Long miembroId, Set<Rol> roles,
			boolean contrasenaTemporal) {
		validar(roles, iglesiaId, miembroId);
		this.correo = Correo.normalizar(correo);
		this.hashContrasena = Objects.requireNonNull(hashContrasena);
		this.iglesiaId = iglesiaId;
		this.miembroId = miembroId;
		this.roles.addAll(roles);
		this.activo = true;
		this.debeCambiarContrasena = contrasenaTemporal;
	}

	/**
	 * @param contrasenaTemporal {@code true} si la contraseña la eligió otra persona (un administrador) y el
	 *     usuario debe cambiarla antes de usar el sistema
	 */
	public static Usuario nuevo(String correo, String hashContrasena, Long iglesiaId, Long miembroId, Set<Rol> roles,
			boolean contrasenaTemporal) {
		return new Usuario(correo, hashContrasena, iglesiaId, miembroId, roles, contrasenaTemporal);
	}

	/**
	 * Actualiza roles, miembro y estado.
	 * @return {@code true} si cambió algo que viaja en el token o que impide el acceso
	 */
	public boolean actualizar(Set<Rol> nuevosRoles, Long nuevoMiembroId, boolean nuevoActivo) {
		validar(nuevosRoles, iglesiaId, nuevoMiembroId);
		boolean cambioAcceso = !roles.equals(nuevosRoles) || !Objects.equals(miembroId, nuevoMiembroId)
				|| (activo && !nuevoActivo);
		roles.clear();
		roles.addAll(nuevosRoles);
		miembroId = nuevoMiembroId;
		activo = nuevoActivo;
		return cambioAcceso;
	}

	/** El propio usuario elige su contraseña: deja de ser temporal. */
	public void cambiarContrasena(String nuevoHash) {
		hashContrasena = Objects.requireNonNull(nuevoHash);
		debeCambiarContrasena = false;
	}

	/** Un administrador fija una contraseña temporal que el usuario debe cambiar antes de usar el sistema. */
	public void restablecerContrasena(String hashTemporal) {
		hashContrasena = Objects.requireNonNull(hashTemporal);
		debeCambiarContrasena = true;
	}

	private static void validar(Set<Rol> roles, Long iglesiaId, Long miembroId) {
		if (roles == null || roles.isEmpty()) {
			throw new ReglaNegocioException("El usuario debe tener al menos un rol");
		}
		Set<RolBase> rolesBase = rolesBase(roles);
		boolean esAdminSistema = rolesBase.contains(RolBase.ADMIN_SISTEMA);
		if (esAdminSistema && (iglesiaId != null || miembroId != null)) {
			throw new ReglaNegocioException("El administrador del sistema no pertenece a ninguna iglesia");
		}
		if (!esAdminSistema && iglesiaId == null) {
			throw new ReglaNegocioException("El usuario debe pertenecer a una iglesia");
		}
		if (miembroId == null && rolesBase.stream().anyMatch(RolBase::requiereMiembro)) {
			throw new ReglaNegocioException(
					"Los roles de líder y miembro, y los que heredan de ellos, requieren un miembro vinculado");
		}
	}

	private static Set<RolBase> rolesBase(Set<Rol> roles) {
		Set<RolBase> rolesBase = EnumSet.noneOf(RolBase.class);
		roles.forEach(rol -> rolesBase.add(rol.rolBaseEfectivo()));
		return rolesBase;
	}

	public Long getId() {
		return id;
	}

	public String getCorreo() {
		return correo;
	}

	public String getHashContrasena() {
		return hashContrasena;
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

	public Long getMiembroId() {
		return miembroId;
	}

	public boolean isActivo() {
		return activo;
	}

	public boolean isDebeCambiarContrasena() {
		return debeCambiarContrasena;
	}

	public Set<Rol> getRoles() {
		return Set.copyOf(roles);
	}

	/** Roles base efectivos de sus roles, sin repetir. */
	public Set<RolBase> getRolesBase() {
		return rolesBase(roles);
	}

	/** Roles que dan acceso. Un rol desactivado sigue asignado pero no aporta perfil ni permisos. */
	public Set<Rol> getRolesActivos() {
		return roles.stream().filter(Rol::isActivo).collect(Collectors.toUnmodifiableSet());
	}

	/** Roles base efectivos de sus roles activos: los que viajan en el claim {@code roles}. */
	public Set<RolBase> getRolesBaseActivos() {
		return rolesBase(getRolesActivos());
	}

	/** Unión de los permisos de sus roles activos. */
	public Set<String> getPermisosActivos() {
		return getRolesActivos().stream()
			.flatMap(rol -> rol.getPermisos().stream())
			.map(Permiso::getCodigo)
			.collect(Collectors.toSet());
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
