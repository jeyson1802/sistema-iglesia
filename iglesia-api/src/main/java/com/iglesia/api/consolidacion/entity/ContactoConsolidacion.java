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
import com.iglesia.api.consolidacion.model.DatosContacto;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;

/** Una llamada, mensaje o visita de un caso. {@code registradoPor} es el usuario que lo anotó. */
@Entity
@Table(name = "contacto_consolidacion")
public class ContactoConsolidacion extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "caso_id", nullable = false, updatable = false)
	private Long casoId;

	@Column(name = "consolidador_id")
	private Long consolidadorId;

	@Column(name = "fecha_hora", nullable = false)
	private Instant fechaHora;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio", nullable = false, length = 14)
	private MedioContacto medio;

	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, length = 16)
	private ResultadoContacto resultado;

	@Column(name = "observaciones", length = LimitesConsolidacion.OBSERVACIONES_MAX)
	private String observaciones;

	@Column(name = "proximo_contacto")
	private LocalDate proximoContacto;

	@Column(name = "registrado_por", nullable = false, updatable = false)
	private Long registradoPor;

	@Column(name = "registrado_en", nullable = false, updatable = false)
	private Instant registradoEn;

	protected ContactoConsolidacion() {
	}

	public static ContactoConsolidacion nuevo(Long casoId, DatosContacto datos, Long usuarioId, Instant ahora) {
		ContactoConsolidacion contacto = new ContactoConsolidacion();
		contacto.casoId = casoId;
		contacto.registradoPor = usuarioId;
		contacto.registradoEn = ahora;
		contacto.actualizar(datos);
		return contacto;
	}

	public void actualizar(DatosContacto datos) {
		fechaHora = datos.fechaHora();
		medio = datos.medio();
		resultado = datos.resultado();
		observaciones = datos.observaciones();
		proximoContacto = datos.proximoContacto();
		consolidadorId = datos.consolidadorId();
	}

	public Long getId() {
		return id;
	}

	public Long getCasoId() {
		return casoId;
	}

	public Long getConsolidadorId() {
		return consolidadorId;
	}

	public Instant getFechaHora() {
		return fechaHora;
	}

	public MedioContacto getMedio() {
		return medio;
	}

	public ResultadoContacto getResultado() {
		return resultado;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public LocalDate getProximoContacto() {
		return proximoContacto;
	}

	public Long getRegistradoPor() {
		return registradoPor;
	}

	public Instant getRegistradoEn() {
		return registradoEn;
	}

}
