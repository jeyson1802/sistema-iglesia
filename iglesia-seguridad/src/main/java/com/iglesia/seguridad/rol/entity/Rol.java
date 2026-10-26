package com.iglesia.seguridad.rol.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.iglesia.seguridad.rol.model.RolBase;

/**
 * Rol de acceso: un conjunto de permisos. Es un rol base (global, con {@code codigo} y sin iglesia) o un
 * rol personalizado de una iglesia, que hereda de un rol base su ámbito y su página de inicio.
 */
@Entity
@Table(name = "rol")
public class Rol {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "iglesia_id", updatable = false)
	private Long iglesiaId;

	@Enumerated(EnumType.STRING)
	@Column(length = 30, updatable = false)
	private RolBase codigo;

	@Column(nullable = false, length = 60)
	private String nombre;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rol_base_id", updatable = false)
	private Rol rolBase;

	@Column(nullable = false)
	private boolean activo;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol_id"),
			inverseJoinColumns = @JoinColumn(name = "permiso_codigo"))
	private Set<Permiso> permisos = new HashSet<>();

	@CreationTimestamp
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@UpdateTimestamp
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@Version
	private long version;

	protected Rol() {
	}

	/** El rol base que define el ámbito y la página de inicio: el propio si es base, o del que hereda. */
	public RolBase rolBaseEfectivo() {
		return esBase() ? codigo : rolBase.rolBaseEfectivo();
	}

	public boolean esBase() {
		return codigo != null;
	}

	public Long getId() {
		return id;
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

	public String getNombre() {
		return nombre;
	}

	public boolean isActivo() {
		return activo;
	}

	public Set<Permiso> getPermisos() {
		return Set.copyOf(permisos);
	}

	@Override
	public boolean equals(Object otro) {
		return this == otro || (otro instanceof Rol rol && id != null && id.equals(rol.getId()));
	}

	@Override
	public int hashCode() {
		return Rol.class.hashCode();
	}

}
