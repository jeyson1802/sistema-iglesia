package com.iglesia.seguridad.actividad.component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.actividad.config.RetencionActividadProperties;
import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository;

/** Borra cada madrugada la actividad más antigua que la retención configurada. */
@Component
class LimpiezaActividad {

	private static final Logger LOG = LoggerFactory.getLogger(LimpiezaActividad.class);

	private final ActividadUsuarioRepository actividades;

	private final RetencionActividadProperties retencion;

	private final Clock reloj;

	LimpiezaActividad(ActividadUsuarioRepository actividades, RetencionActividadProperties retencion, Clock reloj) {
		this.actividades = actividades;
		this.retencion = retencion;
		this.reloj = reloj;
	}

	@Scheduled(cron = "${seguridad.actividad.cron-limpieza}")
	@Transactional
	void limpiar() {
		LocalDate limite = LocalDate.ofInstant(reloj.instant(), ZoneId.systemDefault())
			.minusMonths(retencion.mesesRetencion());
		int borradas = actividades.borrarAnterioresA(limite);
		LOG.info("Limpieza de actividad: {} filas anteriores a {}", borradas, limite);
	}

}
