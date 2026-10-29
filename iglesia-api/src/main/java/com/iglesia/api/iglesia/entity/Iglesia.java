package com.iglesia.api.iglesia.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.iglesia.api.iglesia.constant.AparienciaAdmitida;
import com.iglesia.api.iglesia.constant.LimitesIglesia;
import com.iglesia.api.iglesia.model.Apariencia;
import com.iglesia.api.iglesia.model.DatosIglesia;

/**
 * Iglesia del catálogo de tenants con su identidad (nombres, dirección, contacto) y su apariencia. No extiende
 * {@code EntidadDeIglesia}: es el catálogo mismo, sin {@code iglesia_id} ni RLS. El logo vive en
 * {@code logo_iglesia}; aquí solo se guarda cuándo se subió.
 */
@Entity
@Table(name = "iglesia")
public class Iglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre_comercial", nullable = false, length = LimitesIglesia.NOMBRE_COMERCIAL_MAX)
	private String nombreComercial;

	@Column(name = "denominacion", nullable = false, length = LimitesIglesia.DENOMINACION_MAX)
	private String denominacion;

	@Column(name = "direccion", length = LimitesIglesia.DIRECCION_MAX)
	private String direccion;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "distrito_codigo", length = 6)
	private String distritoCodigo;

	@Column(name = "telefono", length = LimitesIglesia.TELEFONO_MAX)
	private String telefono;

	@Column(name = "correo", length = LimitesIglesia.CORREO_MAX)
	private String correo;

	@Column(name = "sitio_web", length = LimitesIglesia.SITIO_WEB_MAX)
	private String sitioWeb;

	@Column(name = "tema_preset", nullable = false)
	private String temaPreset = AparienciaAdmitida.PRESET_POR_DEFECTO;

	@Column(name = "color_primario", nullable = false)
	private String colorPrimario = AparienciaAdmitida.COLOR_PRIMARIO_POR_DEFECTO;

	@Column(name = "color_superficie")
	private String colorSuperficie;

	@Column(name = "tipografia", nullable = false)
	private String tipografia = AparienciaAdmitida.TIPOGRAFIA_POR_DEFECTO;

	@Column(name = "logo_actualizado_en")
	private Instant logoActualizadoEn;

	@Column(name = "activa", nullable = false, insertable = false, updatable = false)
	private boolean activa = true;

	@CreationTimestamp
	@Column(name = "creada_en", nullable = false, updatable = false)
	private Instant creadaEn;

	protected Iglesia() {
	}

	public static Iglesia nueva(DatosIglesia datos) {
		Iglesia iglesia = new Iglesia();
		iglesia.actualizar(datos);
		return iglesia;
	}

	public void actualizar(DatosIglesia datos) {
		nombreComercial = datos.nombreComercial();
		denominacion = datos.denominacion();
		direccion = datos.direccion();
		distritoCodigo = datos.distritoCodigo();
		telefono = datos.telefono();
		correo = datos.correo();
		sitioWeb = datos.sitioWeb();
	}

	public void cambiarApariencia(Apariencia apariencia) {
		temaPreset = apariencia.preset();
		colorPrimario = apariencia.colorPrimario();
		colorSuperficie = apariencia.colorSuperficie();
		tipografia = apariencia.tipografia();
	}

	public void registrarLogo(Instant subidoEn) {
		logoActualizadoEn = subidoEn;
	}

	public void quitarLogo() {
		logoActualizadoEn = null;
	}

	public Long getId() {
		return id;
	}

	public String getNombreComercial() {
		return nombreComercial;
	}

	public String getDenominacion() {
		return denominacion;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getDistritoCodigo() {
		return distritoCodigo;
	}

	public String getTelefono() {
		return telefono;
	}

	public String getCorreo() {
		return correo;
	}

	public String getSitioWeb() {
		return sitioWeb;
	}

	public Apariencia getApariencia() {
		return new Apariencia(temaPreset, colorPrimario, colorSuperficie, tipografia);
	}

	public Instant getLogoActualizadoEn() {
		return logoActualizadoEn;
	}

	public boolean tieneLogo() {
		return logoActualizadoEn != null;
	}

	public boolean isActiva() {
		return activa;
	}

	public Instant getCreadaEn() {
		return creadaEn;
	}

}
