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
import com.iglesia.api.consolidacion.model.EstadoCaso;

/**
 * Una persona dentro de una ronda (o en un seguimiento individual, sin ronda) con su consolidador y, al cerrarse, el
 * resultado de esa iteración. {@code proximoContacto} es el del último contacto registrado: ordena «Mis asignados».
 */
@Entity
@Table(name = "caso_consolidacion")
public class CasoConsolidacion extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ronda_id", updatable = false)
	private Long rondaId;

	@Column(name = "miembro_id", nullable = false, updatable = false)
	private Long miembroId;

	@Column(name = "consolidador_id")
	private Long consolidadorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 12)
	private EstadoCaso estado = EstadoCaso.PENDIENTE;

	@Column(name = "resultado_id")
	private Long resultadoId;

	@Column(name = "observacion_cierre", length = LimitesConsolidacion.OBSERVACIONES_MAX)
	private String observacionCierre;

	@Column(name = "proximo_contacto")
	private LocalDate proximoContacto;

	@Column(name = "cerrado_en")
	private Instant cerradoEn;

	@Column(name = "cerrado_por")
	private Long cerradoPor;

	@Column(name = "creado_por", nullable = false, updatable = false)
	private Long creadoPor;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected CasoConsolidacion() {
	}

	/** Seguimiento individual (sin ronda), ya asignado. */
	public static CasoConsolidacion individual(Long miembroId, Long consolidadorId, Long usuarioId, Instant ahora) {
		CasoConsolidacion caso = new CasoConsolidacion();
		caso.miembroId = miembroId;
		caso.consolidadorId = consolidadorId;
		caso.creadoPor = usuarioId;
		caso.creadoEn = ahora;
		return caso;
	}

	public void asignar(Long nuevoConsolidadorId) {
		consolidadorId = nuevoConsolidadorId;
	}

	/** Tras registrar, corregir o borrar un contacto: el estado depende de si quedan contactos. */
	public void actualizarContactos(boolean tieneContactos, LocalDate proximo) {
		estado = tieneContactos ? EstadoCaso.EN_CONTACTO : EstadoCaso.PENDIENTE;
		proximoContacto = proximo;
	}

	/** Cierra con un resultado del catálogo, o sin él ({@code null}: «No concluido», al cerrar la ronda). */
	public void cerrar(Long resultado, String observacion, Long usuarioId, Instant ahora) {
		estado = EstadoCaso.CERRADO;
		resultadoId = resultado;
		observacionCierre = observacion;
		cerradoPor = usuarioId;
		cerradoEn = ahora;
		proximoContacto = null;
	}

	public void reabrir(boolean tieneContactos, LocalDate proximo) {
		resultadoId = null;
		observacionCierre = null;
		cerradoPor = null;
		cerradoEn = null;
		actualizarContactos(tieneContactos, proximo);
	}

	public boolean esIndividual() {
		return rondaId == null;
	}

	public Long getId() {
		return id;
	}

	public Long getRondaId() {
		return rondaId;
	}

	public Long getMiembroId() {
		return miembroId;
	}

	public Long getConsolidadorId() {
		return consolidadorId;
	}

	public EstadoCaso getEstado() {
		return estado;
	}

	public Long getResultadoId() {
		return resultadoId;
	}

	public String getObservacionCierre() {
		return observacionCierre;
	}

	public LocalDate getProximoContacto() {
		return proximoContacto;
	}

	public Instant getCerradoEn() {
		return cerradoEn;
	}

	public Long getCerradoPor() {
		return cerradoPor;
	}

	public Long getCreadoPor() {
		return creadoPor;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
