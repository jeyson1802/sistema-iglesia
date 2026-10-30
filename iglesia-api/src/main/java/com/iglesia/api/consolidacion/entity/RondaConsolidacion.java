package com.iglesia.api.consolidacion.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.model.DatosRonda;
import com.iglesia.api.consolidacion.model.EstadoRonda;

/** Tanda de consolidación: un lote de personas repartido entre el equipo durante unas fechas. */
@Entity
@Table(name = "ronda_consolidacion")
public class RondaConsolidacion extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesConsolidacion.NOMBRE_RONDA_MAX)
	private String nombre;

	@Column(name = "descripcion", length = LimitesConsolidacion.DESCRIPCION_RONDA_MAX)
	private String descripcion;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin")
	private LocalDate fechaFin;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 12)
	private EstadoRonda estado = EstadoRonda.PLANIFICADA;

	@Column(name = "creada_por", nullable = false, updatable = false)
	private Long creadaPor;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@Column(name = "cerrada_en")
	private Instant cerradaEn;

	protected RondaConsolidacion() {
	}

	public static RondaConsolidacion nueva(DatosRonda datos, Long usuarioId, Instant ahora) {
		RondaConsolidacion ronda = new RondaConsolidacion();
		ronda.actualizar(datos);
		ronda.creadaPor = usuarioId;
		ronda.creadoEn = ahora;
		return ronda;
	}

	public void actualizar(DatosRonda datos) {
		nombre = datos.nombre();
		descripcion = datos.descripcion();
		fechaInicio = datos.fechaInicio();
		fechaFin = datos.fechaFin();
	}

	/** Pasa al estado siguiente; el servicio ya verificó que la transición es válida. */
	public void pasarA(EstadoRonda destino, Instant ahora) {
		estado = destino;
		if (destino == EstadoRonda.CERRADA) {
			cerradaEn = ahora;
		}
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public LocalDate getFechaFin() {
		return fechaFin;
	}

	public EstadoRonda getEstado() {
		return estado;
	}

	public Long getCreadaPor() {
		return creadaPor;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

	public Instant getCerradaEn() {
		return cerradaEn;
	}

}
