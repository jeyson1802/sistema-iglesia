package com.iglesia.api.asistencia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.asistencia.model.ReglasSeguimiento;

/**
 * Reglas de las alertas de seguimiento de una iglesia. La clave es el propio {@code iglesia_id}, así que no extiende
 * {@code EntidadDeIglesia} (un {@code @Id} no puede ser también {@code @TenantId}): el servicio usa siempre la iglesia
 * del token y la RLS de PostgreSQL impide leer o escribir la de otra.
 */
@Entity
@Table(name = "configuracion_seguimiento")
public class ConfiguracionSeguimiento {

	@Id
	@Column(name = "iglesia_id")
	private Long iglesiaId;

	@Column(name = "faltas_seguidas", nullable = false)
	private short faltasSeguidas;

	@Column(name = "puntos_caida", nullable = false)
	private short puntosCaida;

	@Column(name = "dias_nuevo", nullable = false)
	private short diasNuevo;

	protected ConfiguracionSeguimiento() {
	}

	public ConfiguracionSeguimiento(Long iglesiaId) {
		this.iglesiaId = iglesiaId;
	}

	public void aplicar(ReglasSeguimiento reglas) {
		faltasSeguidas = (short) reglas.faltasSeguidas();
		puntosCaida = (short) reglas.puntosCaida();
		diasNuevo = (short) reglas.diasNuevo();
	}

	public ReglasSeguimiento reglas() {
		return new ReglasSeguimiento(faltasSeguidas, puntosCaida, diasNuevo);
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

}
