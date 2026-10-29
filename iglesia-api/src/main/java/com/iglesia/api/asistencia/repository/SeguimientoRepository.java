package com.iglesia.api.asistencia.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.asistencia.entity.Reunion;

/**
 * Consultas del seguimiento de asistencia. Todas parten de las <b>ocasiones</b> que le corresponden a cada miembro
 * activo desde su ingreso: los días de culto (todas las reuniones de culto de una fecha cuentan como una) y las
 * reuniones de su grupo actual. Las de hoy solo cuentan si ya asistió: mientras el día no termina, no son falta.
 * Se calculan para todos los miembros de la consulta a la vez, con CTE que filtran
 * {@code iglesia_id} en cada paso (incluida la red recursiva del líder), nunca miembro por miembro.
 * <p>
 * Parámetros comunes: {@code hoy}; {@code desde}, el lunes de la semana más antigua analizada; y los filtros
 * opcionales {@code miembroId}, {@code grupoId} y {@code liderId} (su red). Con {@code soloGrupo} cuentan solo las
 * reuniones de grupo. La semana 0 es la actual (de lunes a domingo).
 */
public interface SeguimientoRepository extends Repository<Reunion, Long> {

	String OCASIONES = """
			WITH RECURSIVE red (id) AS (
			    SELECT m.id FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.lider_id = CAST(:liderId AS bigint)
			    UNION
			    SELECT m.id FROM miembro m JOIN red r ON m.lider_id = r.id
			    WHERE m.iglesia_id = :iglesiaId),
			personas AS (
			    SELECT m.id, m.grupo_id, m.lider_id, m.condicion,
			           COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date)) AS inicio
			    FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.activo
			      AND (CAST(:miembroId AS bigint) IS NULL OR m.id = CAST(:miembroId AS bigint))
			      AND (CAST(:grupoId AS bigint) IS NULL OR m.grupo_id = CAST(:grupoId AS bigint))
			      AND (CAST(:liderId AS bigint) IS NULL OR m.id IN (SELECT id FROM red))),
			dias_culto AS (
			    SELECT DISTINCT r.fecha FROM reunion r
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'CULTO' AND r.fecha BETWEEN :desde AND :hoy
			      AND NOT CAST(:soloGrupo AS boolean)),
			ocasiones AS (
			    SELECT p.id AS miembro_id, d.fecha,
			           EXISTS (SELECT 1 FROM asistencia a
			                   JOIN reunion r ON r.iglesia_id = a.iglesia_id AND r.id = a.reunion_id
			                   WHERE a.iglesia_id = :iglesiaId AND a.miembro_id = p.id
			                     AND r.tipo = 'CULTO' AND r.fecha = d.fecha) AS asistio
			    FROM personas p JOIN dias_culto d ON d.fecha >= p.inicio
			    UNION ALL
			    SELECT p.id, r.fecha,
			           EXISTS (SELECT 1 FROM asistencia a
			                   WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = r.id AND a.miembro_id = p.id)
			    FROM personas p
			    JOIN reunion r ON r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.grupo_id = p.grupo_id
			     AND r.fecha BETWEEN GREATEST(CAST(:desde AS date), p.inicio) AND :hoy),
			semanas AS (
			    SELECT o.miembro_id, o.fecha, o.asistio,
			           (CAST(date_trunc('week', CAST(:hoy AS timestamp)) AS date)
			             - CAST(date_trunc('week', CAST(o.fecha AS timestamp)) AS date)) / 7 AS semana,
			           MAX(o.fecha) FILTER (WHERE o.asistio) OVER (PARTITION BY o.miembro_id) AS ultima_asistida
			    FROM ocasiones o
			    WHERE o.fecha < :hoy OR o.asistio)
			""";

	/** Indicadores de cada miembro del alcance; los miembros sin ocasiones vienen con ceros. */
	@Query(value = OCASIONES + """
			, totales AS (
			    SELECT a.miembro_id, COUNT(DISTINCT r.fecha) AS dias, MAX(r.fecha) AS ultima
			    FROM asistencia a JOIN reunion r ON r.iglesia_id = a.iglesia_id AND r.id = a.reunion_id
			    WHERE a.iglesia_id = :iglesiaId AND a.miembro_id IN (SELECT id FROM personas)
			    GROUP BY a.miembro_id)
			SELECT p.id AS miembroId, p.grupo_id AS grupoId, p.lider_id AS liderId, p.inicio AS inicio,
			       COALESCE(p.condicion = 'NUEVO', FALSE) AS nuevo,
			       COUNT(s.fecha) FILTER (WHERE s.semana < 4) AS esperadas4,
			       COUNT(s.fecha) FILTER (WHERE s.semana < 4 AND s.asistio) AS asistidas4,
			       COUNT(s.fecha) FILTER (WHERE s.semana < 8) AS esperadas8,
			       COUNT(s.fecha) FILTER (WHERE s.semana < 8 AND s.asistio) AS asistidas8,
			       COUNT(s.fecha) FILTER (WHERE s.semana BETWEEN 4 AND 11) AS esperadas5a12,
			       COUNT(s.fecha) FILTER (WHERE s.semana BETWEEN 4 AND 11 AND s.asistio) AS asistidas5a12,
			       COUNT(s.fecha) FILTER (WHERE s.semana BETWEEN 8 AND 15) AS esperadas9a16,
			       COUNT(s.fecha) FILTER (WHERE s.semana BETWEEN 8 AND 15 AND s.asistio) AS asistidas9a16,
			       COUNT(s.fecha) FILTER (WHERE NOT s.asistio
			                              AND (s.ultima_asistida IS NULL OR s.fecha > s.ultima_asistida)) AS faltasSeguidas,
			       t.ultima AS ultimaAsistencia, COALESCE(t.dias, 0) AS diasAsistidos
			FROM personas p
			LEFT JOIN semanas s ON s.miembro_id = p.id
			LEFT JOIN totales t ON t.miembro_id = p.id
			GROUP BY p.id, p.grupo_id, p.lider_id, p.inicio, p.condicion, t.ultima, t.dias""", nativeQuery = true)
	List<FilaIndicadores> indicadores(@Param("iglesiaId") long iglesiaId, @Param("hoy") LocalDate hoy,
			@Param("desde") LocalDate desde, @Param("miembroId") Long miembroId, @Param("grupoId") Long grupoId,
			@Param("liderId") Long liderId, @Param("soloGrupo") boolean soloGrupo);

	/** Ocasiones esperadas y asistidas por semana, sumando a todos los miembros del alcance. */
	@Query(value = OCASIONES + """
			SELECT s.semana AS semana, COUNT(*) AS esperadas, COUNT(*) FILTER (WHERE s.asistio) AS asistidas
			FROM semanas s GROUP BY s.semana""", nativeQuery = true)
	List<FilaSemana> porSemana(@Param("iglesiaId") long iglesiaId, @Param("hoy") LocalDate hoy,
			@Param("desde") LocalDate desde, @Param("miembroId") Long miembroId, @Param("grupoId") Long grupoId,
			@Param("liderId") Long liderId, @Param("soloGrupo") boolean soloGrupo);

	/** Reuniones de un grupo y sus asistentes (integrantes o no) por semana. */
	@Query(value = """
			SELECT (CAST(date_trunc('week', CAST(:hoy AS timestamp)) AS date)
			         - CAST(date_trunc('week', CAST(r.fecha AS timestamp)) AS date)) / 7 AS semana,
			       COUNT(DISTINCT r.id) AS reuniones, COUNT(a.miembro_id) AS asistentes
			FROM reunion r
			LEFT JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id
			WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.grupo_id = :grupoId
			  AND r.fecha BETWEEN :desde AND :hoy
			GROUP BY 1""", nativeQuery = true)
	List<FilaReunionesSemana> reunionesDeGrupoPorSemana(@Param("iglesiaId") long iglesiaId,
			@Param("hoy") LocalDate hoy, @Param("desde") LocalDate desde, @Param("grupoId") long grupoId);

	/** Asistentes de cada culto programado por semana. */
	@Query(value = """
			SELECT r.culto_programado_id AS cultoId, c.nombre AS nombre,
			       (CAST(date_trunc('week', CAST(:hoy AS timestamp)) AS date)
			         - CAST(date_trunc('week', CAST(r.fecha AS timestamp)) AS date)) / 7 AS semana,
			       COUNT(a.miembro_id) AS asistentes
			FROM reunion r
			JOIN culto_programado c ON c.iglesia_id = r.iglesia_id AND c.id = r.culto_programado_id
			LEFT JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id
			WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'CULTO' AND r.fecha BETWEEN :desde AND :hoy
			GROUP BY r.culto_programado_id, c.nombre, 3""", nativeQuery = true)
	List<FilaCulto> asistentesPorCulto(@Param("iglesiaId") long iglesiaId, @Param("hoy") LocalDate hoy,
			@Param("desde") LocalDate desde);

	/** Por tipo de reunión (culto o grupo) y semana: personas distintas y asistencias. */
	@Query(value = """
			SELECT r.tipo AS tipo,
			       (CAST(date_trunc('week', CAST(:hoy AS timestamp)) AS date)
			         - CAST(date_trunc('week', CAST(r.fecha AS timestamp)) AS date)) / 7 AS semana,
			       COUNT(DISTINCT a.miembro_id) AS personas, COUNT(a.miembro_id) AS asistencias
			FROM reunion r
			LEFT JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id
			WHERE r.iglesia_id = :iglesiaId AND r.tipo IN ('CULTO', 'GRUPO') AND r.fecha BETWEEN :desde AND :hoy
			GROUP BY r.tipo, 2""", nativeQuery = true)
	List<FilaTipoSemana> personasPorTipoYSemana(@Param("iglesiaId") long iglesiaId, @Param("hoy") LocalDate hoy,
			@Param("desde") LocalDate desde);

	String OCASIONES_DEL_MIEMBRO = """
			WITH miembro_inicio AS (
			    SELECT m.grupo_id,
			           GREATEST(CAST(:desde AS date), COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date))) AS desde
			    FROM miembro m WHERE m.iglesia_id = :iglesiaId AND m.id = :miembroId),
			dias AS (
			    SELECT r.fecha,
			           string_agg(r.nombre, ' · ' ORDER BY r.hora NULLS LAST, r.id) AS descripcion,
			           string_agg(r.nombre, ' · ' ORDER BY r.hora NULLS LAST, r.id)
			               FILTER (WHERE a.miembro_id IS NOT NULL) AS asistio_a
			    FROM reunion r
			    CROSS JOIN miembro_inicio i
			    LEFT JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id AND a.miembro_id = :miembroId
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'CULTO' AND r.fecha BETWEEN i.desde AND :hasta
			    GROUP BY r.fecha),
			ocasiones AS (
			    SELECT d.fecha, CAST(NULL AS time) AS hora, 'CULTO' AS tipo, d.descripcion, d.asistio_a,
			           CASE WHEN d.asistio_a IS NULL THEN 'FALTO' ELSE 'ASISTIO' END AS estado
			    FROM dias d
			    UNION ALL
			    SELECT r.fecha, r.hora, r.tipo, r.nombre,
			           CASE WHEN a.miembro_id IS NULL THEN NULL ELSE r.nombre END,
			           CASE WHEN a.miembro_id IS NULL THEN 'FALTO' ELSE 'ASISTIO' END
			    FROM reunion r
			    JOIN miembro_inicio i ON r.grupo_id = i.grupo_id
			    LEFT JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id AND a.miembro_id = :miembroId
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.fecha BETWEEN i.desde AND :hasta
			    UNION ALL
			    SELECT r.fecha, r.hora, r.tipo, r.nombre, r.nombre, 'EXTRA'
			    FROM reunion r
			    CROSS JOIN miembro_inicio i
			    JOIN asistencia a ON a.iglesia_id = r.iglesia_id AND a.reunion_id = r.id AND a.miembro_id = :miembroId
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'EVENTO' AND r.fecha BETWEEN i.desde AND :hasta)
			""";

	/** Filtra por estado; lo de hoy al que aún no asistió no es falta todavía. */
	String FILTRO_ESTADO = "\n" + """
			 WHERE (CAST(:estado AS varchar) IS NULL OR o.estado = CAST(:estado AS varchar))
			   AND (o.fecha < :hoy OR o.estado <> 'FALTO')""";

	/** Historial de ocasiones de un miembro, lo más reciente primero. */
	@Query(value = OCASIONES_DEL_MIEMBRO + """
			SELECT o.fecha AS fecha, o.hora AS hora, o.tipo AS tipo, o.descripcion AS descripcion,
			       o.asistio_a AS asistioA, o.estado AS estado
			FROM ocasiones o""" + FILTRO_ESTADO + "\nORDER BY o.fecha DESC, o.hora DESC NULLS LAST",
			countQuery = OCASIONES_DEL_MIEMBRO + "SELECT COUNT(*) FROM ocasiones o" + FILTRO_ESTADO,
			nativeQuery = true)
	Page<FilaOcasion> ocasionesDelMiembro(@Param("iglesiaId") long iglesiaId, @Param("miembroId") long miembroId,
			@Param("hoy") LocalDate hoy, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("estado") String estado, Pageable paginacion);

	interface FilaIndicadores {

		Long getMiembroId();

		Long getGrupoId();

		Long getLiderId();

		LocalDate getInicio();

		boolean getNuevo();

		long getEsperadas4();

		long getAsistidas4();

		long getEsperadas8();

		long getAsistidas8();

		long getEsperadas5a12();

		long getAsistidas5a12();

		long getEsperadas9a16();

		long getAsistidas9a16();

		int getFaltasSeguidas();

		LocalDate getUltimaAsistencia();

		long getDiasAsistidos();

	}

	interface FilaSemana {

		int getSemana();

		long getEsperadas();

		long getAsistidas();

	}

	interface FilaReunionesSemana {

		int getSemana();

		long getReuniones();

		long getAsistentes();

	}

	interface FilaCulto {

		Long getCultoId();

		String getNombre();

		int getSemana();

		long getAsistentes();

	}

	interface FilaTipoSemana {

		String getTipo();

		int getSemana();

		long getPersonas();

		long getAsistencias();

	}

	interface FilaOcasion {

		LocalDate getFecha();

		LocalTime getHora();

		String getTipo();

		String getDescripcion();

		String getAsistioA();

		String getEstado();

	}

}
