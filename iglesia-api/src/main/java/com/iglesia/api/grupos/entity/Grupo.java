package com.iglesia.api.grupos.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.grupos.constant.LimitesGrupo;
import com.iglesia.api.grupos.model.DatosGrupo;
import com.iglesia.api.grupos.model.DiaSemana;

/**
 * Grupo pequeño de la iglesia («célula», «hogar de paz»...): se reúne una vez por semana y lo lidera un miembro. No
 * tiene nombre: es «el grupo de» su líder, obligatorio y con un solo grupo. El líder y el anfitrión se guardan por id;
 * los valida el contexto de miembros.
 */
@Entity
@Table(name = "grupo")
public class Grupo extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "lider_id", nullable = false)
	private Long liderId;

	@Column(name = "anfitrion_id")
	private Long anfitrionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "dia_reunion", length = 10)
	private DiaSemana diaReunion;

	@Column(name = "hora_reunion")
	private LocalTime horaReunion;

	@Column(name = "direccion", length = LimitesGrupo.DIRECCION_MAX)
	private String direccion;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "distrito_codigo", length = 6)
	private String distritoCodigo;

	@Column(name = "descripcion", length = LimitesGrupo.DESCRIPCION_MAX)
	private String descripcion;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected Grupo() {
	}

	public static Grupo nuevo(DatosGrupo datos) {
		Grupo grupo = new Grupo();
		grupo.actualizar(datos);
		return grupo;
	}

	public void actualizar(DatosGrupo datos) {
		liderId = datos.liderId();
		anfitrionId = datos.anfitrionId();
		diaReunion = datos.diaReunion();
		horaReunion = datos.horaReunion();
		direccion = datos.direccion();
		distritoCodigo = datos.distritoCodigo();
		descripcion = datos.descripcion();
	}

	public void cambiarEstado(boolean nuevoEstado) {
		activo = nuevoEstado;
	}

	public Long getId() {
		return id;
	}

	public Long getLiderId() {
		return liderId;
	}

	public Long getAnfitrionId() {
		return anfitrionId;
	}

	public DiaSemana getDiaReunion() {
		return diaReunion;
	}

	public LocalTime getHoraReunion() {
		return horaReunion;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getDistritoCodigo() {
		return distritoCodigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public boolean isActivo() {
		return activo;
	}

}
