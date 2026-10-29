package com.iglesia.api.asistencia.entity;

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
import com.iglesia.api.asistencia.model.DatosCulto;
import com.iglesia.api.common.multiiglesia.entity.EntidadDeIglesia;
import com.iglesia.api.grupos.model.DiaSemana;

/** Culto fijo de la iglesia (p. ej. «Culto dominical», domingos 10:00). Se desactiva, no se borra. */
@Entity
@Table(name = "culto_programado")
public class CultoProgramado extends EntidadDeIglesia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nombre", nullable = false, length = LimitesAsistencia.NOMBRE_CULTO_MAX)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", nullable = false, length = 10)
	private DiaSemana diaSemana;

	@Column(name = "hora", nullable = false)
	private LocalTime hora;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	protected CultoProgramado() {
	}

	public static CultoProgramado nuevo(DatosCulto datos) {
		CultoProgramado culto = new CultoProgramado();
		culto.actualizar(datos);
		return culto;
	}

	public void actualizar(DatosCulto datos) {
		nombre = datos.nombre();
		diaSemana = datos.diaSemana();
		hora = datos.hora();
	}

	public void cambiarEstado(boolean nuevoEstado) {
		activo = nuevoEstado;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public DiaSemana getDiaSemana() {
		return diaSemana;
	}

	public LocalTime getHora() {
		return hora;
	}

	public boolean isActivo() {
		return activo;
	}

}
