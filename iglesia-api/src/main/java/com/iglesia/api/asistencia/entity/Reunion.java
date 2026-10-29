package com.iglesia.api.asistencia.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;

/**
 * Ocurrencia concreta donde se toma asistencia. El nombre se copia del culto, del grupo o del evento para que el
 * historial no cambie si luego se renombran. El culto y el grupo se guardan por id (el grupo pertenece a otro
 * contexto). Las reuniones de cultos y grupos las crea {@code ReunionRepository#abrirDeCulto} y
 * {@code #abrirDeGrupo} con {@code ON CONFLICT}, así que esta clase solo crea eventos.
 */
@Entity
@Table(name = "reunion")
public class Reunion extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10, updatable = false)
	private TipoReunion tipo;

	@Column(name = "nombre", nullable = false, length = LimitesAsistencia.NOMBRE_EVENTO_MAX)
	private String nombre;

	@Column(name = "fecha", nullable = false)
	private LocalDate fecha;

	@Column(name = "hora")
	private LocalTime hora;

	@Column(name = "culto_programado_id", updatable = false)
	private Long cultoProgramadoId;

	@Column(name = "grupo_id", updatable = false)
	private Long grupoId;

	@Column(name = "creada_por", nullable = false, updatable = false)
	private Long creadaPor;

	@Column(name = "creada_en", nullable = false, updatable = false)
	private Instant creadaEn;

	protected Reunion() {
	}

	public static Reunion evento(String nombre, LocalDate fecha, LocalTime hora, Long usuarioId, Instant ahora) {
		Reunion reunion = new Reunion();
		reunion.tipo = TipoReunion.EVENTO;
		reunion.nombre = nombre;
		reunion.fecha = fecha;
		reunion.hora = hora;
		reunion.creadaPor = usuarioId;
		reunion.creadaEn = ahora;
		return reunion;
	}

	public Long getId() {
		return id;
	}

	public TipoReunion getTipo() {
		return tipo;
	}

	public String getNombre() {
		return nombre;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public LocalTime getHora() {
		return hora;
	}

	public Long getCultoProgramadoId() {
		return cultoProgramadoId;
	}

	public Long getGrupoId() {
		return grupoId;
	}

	public Long getCreadaPor() {
		return creadaPor;
	}

	public Instant getCreadaEn() {
		return creadaEn;
	}

}
