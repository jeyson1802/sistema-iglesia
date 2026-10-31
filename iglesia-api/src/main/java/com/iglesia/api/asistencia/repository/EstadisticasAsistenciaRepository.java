package com.iglesia.api.asistencia.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.common.estadisticas.constant.ConsultasAlcance;

/**
 * Cifras agregadas de asistencia de la iglesia o de la red de un líder ({@code liderId} nulo: toda la iglesia). En
 * la red solo cuentan los asistentes que son de ella.
 */
public interface EstadisticasAsistenciaRepository extends Repository<Reunion, Long> {

	/** Cultos y asistentes del alcance por día, semana o mes ({@code unidad}: {@code day}, {@code week} o {@code month}). */
	@Query(value = ConsultasAlcance.ALCANCE + """
			, cultos AS (
			    SELECT r.id, r.fecha FROM reunion r
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'CULTO' AND r.fecha BETWEEN :desde AND :hasta
			),
			conteos AS (
			    SELECT c.fecha,
			           (SELECT count(*) FROM asistencia a JOIN alcance m ON m.id = a.miembro_id
			            WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = c.id) AS asistentes
			    FROM cultos c
			)
			SELECT CAST(date_trunc(:unidad, CAST(fecha AS timestamp)) AS date) AS inicio, count(*) AS cultos,
			       sum(asistentes) AS asistentes
			FROM conteos GROUP BY 1 ORDER BY 1""", nativeQuery = true)
	List<FilaCultos> cultos(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("unidad") String unidad);

	/** Personas distintas del alcance que asistieron a cualquier reunión del rango. */
	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT count(DISTINCT a.miembro_id) FROM asistencia a
			JOIN alcance m ON m.id = a.miembro_id
			JOIN reunion r ON r.id = a.reunion_id AND r.iglesia_id = :iglesiaId
			WHERE a.iglesia_id = :iglesiaId AND r.fecha BETWEEN :desde AND :hasta""", nativeQuery = true)
	long personasQueAsistieron(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	/** Quienes llegaron en el rango y cuántos de ellos asistieron a alguna reunión después del día en que llegaron. */
	@Query(value = ConsultasAlcance.ALCANCE + """
			, llegados AS (
			    SELECT id, COALESCE(fecha_ingreso, CAST(creado_en AS date)) AS fecha FROM alcance
			    WHERE COALESCE(fecha_ingreso, CAST(creado_en AS date)) BETWEEN :desde AND :hasta
			)
			SELECT count(*) AS llegaron,
			       count(*) FILTER (WHERE EXISTS (
			           SELECT 1 FROM asistencia a JOIN reunion r ON r.id = a.reunion_id AND r.iglesia_id = :iglesiaId
			           WHERE a.iglesia_id = :iglesiaId AND a.miembro_id = l.id AND r.fecha > l.fecha)) AS volvieron
			FROM llegados l""", nativeQuery = true)
	FilaRetorno retorno(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	interface FilaCultos {

		LocalDate getInicio();

		long getCultos();

		long getAsistentes();

	}

	interface FilaRetorno {

		long getLlegaron();

		long getVolvieron();

	}

}
