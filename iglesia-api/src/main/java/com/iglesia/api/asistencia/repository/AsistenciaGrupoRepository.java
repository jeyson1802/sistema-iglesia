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
 * Asistencia de las reuniones de un grupo, persona por persona. A un integrante (miembro activo del grupo) le
 * corresponde una reunión desde su ingreso a la iglesia, igual que en el seguimiento; si asistió, cuenta aunque haya
 * sido antes. Quien asistió sin ser integrante es visitante. Cada paso filtra {@code iglesia_id}.
 */
public interface AsistenciaGrupoRepository extends Repository<Reunion, Long> {

	/** Integrantes activos del grupo con el día desde el que les corresponden las reuniones. */
	String INTEGRANTES = """
			WITH integrantes AS (
			    SELECT m.id, COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date)) AS inicio
			    FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.activo AND m.grupo_id = :grupoId)
			""";

	/** Reuniones del grupo con sus conteos, la más reciente primero. */
	@Query(value = INTEGRANTES + """
			, conteos AS (
			    SELECT r.id, r.fecha, r.hora,
			           (SELECT COUNT(*) FROM asistencia a JOIN integrantes i ON i.id = a.miembro_id
			            WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = r.id) AS asistieron,
			           (SELECT COUNT(*) FROM integrantes i
			            WHERE i.inicio <= r.fecha
			              AND NOT EXISTS (SELECT 1 FROM asistencia a
			                              WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = r.id
			                                AND a.miembro_id = i.id)) AS faltaron,
			           (SELECT COUNT(*) FROM asistencia a
			            WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = r.id
			              AND a.miembro_id NOT IN (SELECT i.id FROM integrantes i)) AS visitantes
			    FROM reunion r
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.grupo_id = :grupoId)
			SELECT c.id AS reunionId, c.fecha AS fecha, c.hora AS hora, c.asistieron AS asistieron,
			       c.faltaron AS faltaron, c.visitantes AS visitantes
			FROM conteos c
			ORDER BY c.fecha DESC, c.hora DESC NULLS LAST""",
			countQuery = """
					SELECT COUNT(*) FROM reunion r
					WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.grupo_id = :grupoId""",
			nativeQuery = true)
	Page<FilaReunion> reuniones(@Param("iglesiaId") long iglesiaId, @Param("grupoId") long grupoId,
			Pageable paginacion);

	/** Integrantes y visitantes de una reunión del grupo, con cómo quedó cada uno, por nombre. */
	@Query(value = INTEGRANTES + """
			, presentes AS (
			    SELECT a.miembro_id FROM asistencia a
			    WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = :reunionId)
			SELECT m.id AS miembroId, m.nombres || ' ' || m.apellidos AS nombreCompleto, n.nombre AS nivel,
			       CASE WHEN m.id NOT IN (SELECT i.id FROM integrantes i) THEN 'VISITANTE'
			            WHEN m.id IN (SELECT p.miembro_id FROM presentes p) THEN 'ASISTIO'
			            ELSE 'FALTO' END AS estado
			FROM miembro m
			LEFT JOIN nivel n ON n.iglesia_id = m.iglesia_id AND n.id = m.nivel_id
			WHERE m.iglesia_id = :iglesiaId
			  AND (m.id IN (SELECT p.miembro_id FROM presentes p)
			       OR m.id IN (SELECT i.id FROM integrantes i WHERE i.inicio <= :fecha))
			ORDER BY m.nombres, m.apellidos""", nativeQuery = true)
	List<FilaPersona> personas(@Param("iglesiaId") long iglesiaId, @Param("grupoId") long grupoId,
			@Param("reunionId") long reunionId, @Param("fecha") LocalDate fecha);

	interface FilaReunion {

		Long getReunionId();

		LocalDate getFecha();

		LocalTime getHora();

		long getAsistieron();

		long getFaltaron();

		long getVisitantes();

	}

	interface FilaPersona {

		Long getMiembroId();

		String getNombreCompleto();

		String getNivel();

		String getEstado();

	}

}
