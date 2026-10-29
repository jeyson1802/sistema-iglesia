package com.iglesia.api.aportes.entity;

import java.math.BigDecimal;
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

import com.iglesia.api.aportes.constant.LimitesAporte;
import com.iglesia.api.aportes.model.DatosAporte;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;

/**
 * Aporte de un miembro (o anónimo, sin {@code miembroId}). Nunca se borra: se anula con un motivo. Guarda quién lo
 * registró y quién lo modificó o anuló por última vez, como {@code usuarioId} del token (el usuario vive en
 * iglesia-seguridad, por eso no es una referencia). El aportante se guarda por id: pertenece al contexto de miembros.
 */
@Entity
@Table(name = "aporte")
public class Aporte extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fecha", nullable = false)
	private LocalDate fecha;

	@Column(name = "miembro_id")
	private Long miembroId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaAporte categoria;

	@Column(name = "monto", nullable = false, precision = LimitesAporte.MONTO_ENTEROS + LimitesAporte.MONTO_DECIMALES,
			scale = LimitesAporte.MONTO_DECIMALES)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio", nullable = false, length = 15)
	private MedioAporte medio;

	@Column(name = "numero_operacion", length = LimitesAporte.NUMERO_OPERACION_MAX)
	private String numeroOperacion;

	@Column(name = "peticion", length = LimitesAporte.PETICION_MAX)
	private String peticion;

	@Column(name = "observacion", length = LimitesAporte.OBSERVACION_MAX)
	private String observacion;

	@Column(name = "anulado", nullable = false)
	private boolean anulado;

	@Column(name = "motivo_anulacion", length = LimitesAporte.MOTIVO_ANULACION_MAX)
	private String motivoAnulacion;

	@Column(name = "registrado_por", nullable = false, updatable = false)
	private Long registradoPor;

	@Column(name = "registrado_en", nullable = false, updatable = false)
	private Instant registradoEn;

	@Column(name = "modificado_por")
	private Long modificadoPor;

	@Column(name = "modificado_en")
	private Instant modificadoEn;

	protected Aporte() {
	}

	public static Aporte nuevo(DatosAporte datos, CategoriaAporte categoria, Long usuarioId, Instant ahora) {
		Aporte aporte = new Aporte();
		aporte.asignar(datos, categoria);
		aporte.registradoPor = usuarioId;
		aporte.registradoEn = ahora;
		return aporte;
	}

	public void corregir(DatosAporte datos, CategoriaAporte nuevaCategoria, Long usuarioId, Instant ahora) {
		asignar(datos, nuevaCategoria);
		marcarModificado(usuarioId, ahora);
	}

	public void anular(String motivo, Long usuarioId, Instant ahora) {
		anulado = true;
		motivoAnulacion = motivo;
		marcarModificado(usuarioId, ahora);
	}

	private void asignar(DatosAporte datos, CategoriaAporte nuevaCategoria) {
		fecha = datos.fecha();
		miembroId = datos.miembroId();
		categoria = nuevaCategoria;
		monto = datos.monto();
		medio = datos.medio();
		numeroOperacion = datos.numeroOperacion();
		peticion = datos.peticion();
		observacion = datos.observacion();
	}

	private void marcarModificado(Long usuarioId, Instant ahora) {
		modificadoPor = usuarioId;
		modificadoEn = ahora;
	}

	public Long getId() {
		return id;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public Long getMiembroId() {
		return miembroId;
	}

	public CategoriaAporte getCategoria() {
		return categoria;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public MedioAporte getMedio() {
		return medio;
	}

	public String getNumeroOperacion() {
		return numeroOperacion;
	}

	public String getPeticion() {
		return peticion;
	}

	public String getObservacion() {
		return observacion;
	}

	public boolean isAnulado() {
		return anulado;
	}

	public String getMotivoAnulacion() {
		return motivoAnulacion;
	}

	public Long getRegistradoPor() {
		return registradoPor;
	}

	public Instant getRegistradoEn() {
		return registradoEn;
	}

	public Long getModificadoPor() {
		return modificadoPor;
	}

	public Instant getModificadoEn() {
		return modificadoEn;
	}

}
