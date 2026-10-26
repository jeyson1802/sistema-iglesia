package com.iglesia.seguridad.usuario.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
import com.iglesia.seguridad.usuario.model.Correo;
import com.iglesia.seguridad.usuario.model.Rol;

/**
 * Cuenta de acceso. Protege sus invariantes: el administrador del sistema no pertenece a ninguna
 * iglesia, el resto sí, y los roles de líder y miembro exigen un miembro vinculado.
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

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "rol", nullable = false, length = 30)
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

	private Usuario(String correo, String hashContrasena, Long iglesiaId, Long miembroId, Set<Rol> roles) {
		validar(roles, iglesiaId, miembroId);
		this.correo = Correo.normalizar(correo);
		this.hashContrasena = Objects.requireNonNull(hashContrasena);
		this.iglesiaId = iglesiaId;
		this.miembroId = miembroId;
		this.roles.addAll(roles);
		this.activo = true;
	}

	public static Usuario nuevo(String correo, String hashContrasena, Long iglesiaId, Long miembroId, Set<Rol> roles) {
		return new Usuario(correo, hashContrasena, iglesiaId, miembroId, roles);
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

	public void cambiarContrasena(String nuevoHash) {
		hashContrasena = Objects.requireNonNull(nuevoHash);
	}

	private static void validar(Set<Rol> roles, Long iglesiaId, Long miembroId) {
		if (roles == null || roles.isEmpty()) {
			throw new ReglaNegocioException("El usuario debe tener al menos un rol");
		}
		boolean esAdminSistema = roles.contains(Rol.ADMIN_SISTEMA);
		if (esAdminSistema && (iglesiaId != null || miembroId != null)) {
			throw new ReglaNegocioException("El administrador del sistema no pertenece a ninguna iglesia");
		}
		if (!esAdminSistema && iglesiaId == null) {
			throw new ReglaNegocioException("El usuario debe pertenecer a una iglesia");
		}
		if (miembroId == null && roles.stream().anyMatch(Rol::requiereMiembro)) {
			throw new ReglaNegocioException("Los roles de líder y miembro requieren un miembro vinculado");
		}
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

	public Set<Rol> getRoles() {
		return Set.copyOf(roles);
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
