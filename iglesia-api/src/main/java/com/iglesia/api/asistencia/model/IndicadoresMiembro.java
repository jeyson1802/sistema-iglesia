package com.iglesia.api.asistencia.model;

import java.time.LocalDate;

/**
 * Indicadores de asistencia de un miembro calculados en la base sobre las últimas
 * {@value com.iglesia.api.asistencia.constant.LimitesSeguimiento#SEMANAS_ANALISIS} semanas.
 *
 * @param inicio desde cuándo le corresponden ocasiones (fecha de ingreso o de registro)
 * @param nuevo si su condición es «Nuevo»
 * @param ultimas4 semanas 0 a 3
 * @param ultimas8 semanas 0 a 7: el porcentaje que se muestra
 * @param semanas5a12 semanas 4 a 11: la base de la alerta de caída
 * @param semanas9a16 semanas 8 a 15: la tendencia frente a las 8 últimas
 * @param faltasSeguidas ocasiones seguidas sin asistir hasta hoy
 * @param ultimaAsistencia última fecha en que asistió a cualquier reunión (eventos incluidos), o {@code null}
 * @param diasAsistidos días distintos en que asistió alguna vez
 */
public record IndicadoresMiembro(Long miembroId, Long grupoId, Long liderId, LocalDate inicio, boolean nuevo,
		Asistidas ultimas4, Asistidas ultimas8, Asistidas semanas5a12, Asistidas semanas9a16, int faltasSeguidas,
		LocalDate ultimaAsistencia, long diasAsistidos) {
}
