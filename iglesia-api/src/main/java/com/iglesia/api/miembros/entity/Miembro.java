package com.iglesia.api.miembros.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.miembros.model.DatosFicha;
import com.iglesia.api.miembros.model.EstadoCivil;
import com.iglesia.api.miembros.model.Genero;
import com.iglesia.api.miembros.model.NivelEstudios;
import com.iglesia.api.niveles.entity.Nivel;

/**
 * Congregante de la iglesia: entidad central del sistema. Solo nombres y apellidos son obligatorios; el resto de la
 * ficha se completa con el tiempo. El líder es una autorrelación ({@code lider_id}); el grupo se guarda por id porque
 * el contexto de grupos depende de este y no al revés.
 */
@Entity
@Table(name = "miembro")
public class Miembro extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombres", nullable = false, length = LimitesMiembro.NOMBRES_MAX)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = LimitesMiembro.APELLIDOS_MAX)
	private String apellidos;

	@Enumerated(EnumType.STRING)
	@Column(name = "genero", length = 10)
	private Genero genero;

	@Column(name = "fecha_nacimiento")
	private LocalDate fechaNacimiento;

	@Column(name = "anio_nacimiento_estimado")
	private Short anioNacimientoEstimado;

	@Column(name = "fecha_ingreso")
	private LocalDate fechaIngreso;

	@Enumerated(EnumType.STRING)
	@Column(name = "condicion", length = 12)
	private CondicionMiembro condicion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_civil", length = 12)
	private EstadoCivil estadoCivil;

	@Column(name = "telefono", length = LimitesMiembro.TELEFONO_MAX)
	private String telefono;

	@Column(name = "correo", length = LimitesMiembro.CORREO_MAX)
	private String correo;

	@Column(name = "direccion", length = LimitesMiembro.DIRECCION_MAX)
	private String direccion;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "distrito_codigo", length = 6)
	private String distritoCodigo;

	@Column(name = "ocupacion", length = LimitesMiembro.OCUPACION_MAX)
	private String ocupacion;

	@Column(name = "profesion", length = LimitesMiembro.PROFESION_MAX)
	private String profesion;

	@Enumerated(EnumType.STRING)
	@Column(name = "nivel_estudios", length = 15)
	private NivelEstudios nivelEstudios;

	@Column(name = "centro_estudios", length = LimitesMiembro.CENTRO_ESTUDIOS_MAX)
	private String centroEstudios;

	@Column(name = "peticion", length = LimitesMiembro.PETICION_MAX)
	private String peticion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "nivel_id")
	private Nivel nivel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lider_id")
	private Miembro lider;

	@Column(name = "grupo_id")
	private Long grupoId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invitado_por_id")
	private Miembro invitadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registrado_por_id")
	private Miembro registradoPor;

	@Column(name = "es_pastor_principal", nullable = false)
	private boolean pastorPrincipal;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@CreationTimestamp
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@UpdateTimestamp
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected Miembro() {
	}

	public static Miembro nuevo(DatosFicha datos) {
		Miembro miembro = new Miembro();
		miembro.actualizar(datos);
		return miembro;
	}

	public void actualizar(DatosFicha datos) {
		nombres = datos.nombres();
		apellidos = datos.apellidos();
		genero = datos.genero();
		fechaNacimiento = datos.fechaNacimiento();
		anioNacimientoEstimado = datos.anioNacimientoEstimado();
		fechaIngreso = datos.fechaIngreso();
		condicion = datos.condicion();
		estadoCivil = datos.estadoCivil();
		telefono = datos.telefono();
		correo = datos.correo();
		direccion = datos.direccion();
		distritoCodigo = datos.distritoCodigo();
		ocupacion = datos.ocupacion();
		profesion = datos.profesion();
		nivelEstudios = datos.nivelEstudios();
		centroEstudios = datos.centroEstudios();
		peticion = datos.peticion();
		nivel = datos.nivel();
		lider = datos.lider();
		grupoId = datos.grupoId();
		invitadoPor = datos.invitadoPor();
		registradoPor = datos.registradoPor();
	}

	/** Marca al miembro como pastor principal: no tiene líder. */
	public void designarPastorPrincipal() {
		pastorPrincipal = true;
		lider = null;
	}

	public void quitarPastorPrincipal() {
		pastorPrincipal = false;
	}

	public void cambiarEstado(boolean nuevoEstado) {
		activo = nuevoEstado;
	}

	public String nombreCompleto() {
		return nombres + " " + apellidos;
	}

	public Long getId() {
		return id;
	}

	public String getNombres() {
		return nombres;
	}

	public String getApellidos() {
		return apellidos;
	}

	public Genero getGenero() {
		return genero;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public Short getAnioNacimientoEstimado() {
		return anioNacimientoEstimado;
	}

	public LocalDate getFechaIngreso() {
		return fechaIngreso;
	}

	public CondicionMiembro getCondicion() {
		return condicion;
	}

	public EstadoCivil getEstadoCivil() {
		return estadoCivil;
	}

	public String getTelefono() {
		return telefono;
	}

	public String getCorreo() {
		return correo;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getDistritoCodigo() {
		return distritoCodigo;
	}

	public String getOcupacion() {
		return ocupacion;
	}

	public String getProfesion() {
		return profesion;
	}

	public NivelEstudios getNivelEstudios() {
		return nivelEstudios;
	}

	public String getCentroEstudios() {
		return centroEstudios;
	}

	public String getPeticion() {
		return peticion;
	}

	public Nivel getNivel() {
		return nivel;
	}

	public Miembro getLider() {
		return lider;
	}

	public Long getGrupoId() {
		return grupoId;
	}

	public Miembro getInvitadoPor() {
		return invitadoPor;
	}

	public Miembro getRegistradoPor() {
		return registradoPor;
	}

	public boolean isPastorPrincipal() {
		return pastorPrincipal;
	}

	public boolean isActivo() {
		return activo;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

	public Instant getActualizadoEn() {
		return actualizadoEn;
	}

}
