package com.iglesia.api.grupos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.iglesia.api.grupos.constant.LimitesGrupo;

/**
 * Cómo llama la iglesia a sus grupos. La clave es el propio {@code iglesia_id}, así que no extiende
 * {@code EntidadDeIglesia} (un {@code @Id} no puede ser también {@code @TenantId}): el servicio usa siempre la iglesia
 * del token y la RLS de PostgreSQL impide leer o escribir la de otra.
 */
@Entity
@Table(name = "configuracion_iglesia")
public class ConfiguracionIglesia {

	@Id
	@Column(name = "iglesia_id")
	private Long iglesiaId;

	@Column(name = "nombre_grupo_singular", nullable = false, length = LimitesGrupo.NOMBRE_TIPO_MAX)
	private String nombreGrupoSingular;

	@Column(name = "nombre_grupo_plural", nullable = false, length = LimitesGrupo.NOMBRE_TIPO_MAX)
	private String nombreGrupoPlural;

	protected ConfiguracionIglesia() {
	}

	public ConfiguracionIglesia(Long iglesiaId) {
		this.iglesiaId = iglesiaId;
	}

	public void nombrarGrupos(String singular, String plural) {
		nombreGrupoSingular = singular;
		nombreGrupoPlural = plural;
	}

	public Long getIglesiaId() {
		return iglesiaId;
	}

	public String getNombreGrupoSingular() {
		return nombreGrupoSingular;
	}

	public String getNombreGrupoPlural() {
		return nombreGrupoPlural;
	}

}
