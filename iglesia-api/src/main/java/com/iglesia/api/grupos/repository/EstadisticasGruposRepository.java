package com.iglesia.api.grupos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.common.estadisticas.constant.ConsultasAlcance;
import com.iglesia.api.grupos.entity.Grupo;

/**
 * Cifras de los grupos activos de la iglesia o, para un líder ({@code liderId}), de los que lidera él o alguien de su
 * red. A cada reunión de grupo le corresponden los integrantes activos que ya habían llegado ese día.
 */
public interface EstadisticasGruposRepository extends Repository<Grupo, Long> {

	String GRUPOS_DEL_ALCANCE = ConsultasAlcance.ALCANCE + """
			, grupos_alcance AS (
			    SELECT g.id, g.lider_id FROM grupo g
			    WHERE g.iglesia_id = :iglesiaId AND g.activo
			      AND (CAST(:liderId AS bigint) IS NULL OR g.lider_id = CAST(:liderId AS bigint)
			           OR g.lider_id IN (SELECT id FROM red))
			),
			integrantes AS (
			    SELECT m.id, m.grupo_id, COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date)) AS inicio
			    FROM miembro m JOIN grupos_alcance g ON g.id = m.grupo_id
			    WHERE m.iglesia_id = :iglesiaId AND m.activo
			)
			""";

	/** Cada grupo con sus integrantes y las reuniones y ocasiones (esperadas y asistidas) del rango. */
	@Query(value = GRUPOS_DEL_ALCANCE + """
			, reuniones AS (
			    SELECT r.id, r.grupo_id, r.fecha FROM reunion r JOIN grupos_alcance g ON g.id = r.grupo_id
			    WHERE r.iglesia_id = :iglesiaId AND r.tipo = 'GRUPO' AND r.fecha BETWEEN :desde AND :hasta
			),
			ocasiones AS (
			    SELECT r.grupo_id,
			           EXISTS (SELECT 1 FROM asistencia a
			                   WHERE a.iglesia_id = :iglesiaId AND a.reunion_id = r.id AND a.miembro_id = i.id) AS asistio
			    FROM reuniones r JOIN integrantes i ON i.grupo_id = r.grupo_id AND i.inicio <= r.fecha
			)
			SELECT g.id AS grupoId,
			       CASE WHEN l.id IS NULL THEN NULL ELSE l.nombres || ' ' || l.apellidos END AS liderNombre,
			       (SELECT count(*) FROM integrantes i WHERE i.grupo_id = g.id) AS integrantes,
			       (SELECT count(*) FROM reuniones r WHERE r.grupo_id = g.id) AS reuniones,
			       (SELECT count(*) FROM ocasiones o WHERE o.grupo_id = g.id) AS esperadas,
			       (SELECT count(*) FROM ocasiones o WHERE o.grupo_id = g.id AND o.asistio) AS asistidas
			FROM grupos_alcance g
			LEFT JOIN miembro l ON l.id = g.lider_id AND l.iglesia_id = :iglesiaId""", nativeQuery = true)
	List<FilaGrupo> porGrupo(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	/** Miembros activos del alcance y cuántos de ellos están en un grupo. */
	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT count(*) AS activos, count(*) FILTER (WHERE grupo_id IS NOT NULL) AS enGrupo
			FROM alcance WHERE activo""", nativeQuery = true)
	FilaCobertura cobertura(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId);

	interface FilaGrupo {

		Long getGrupoId();

		String getLiderNombre();

		long getIntegrantes();

		long getReuniones();

		long getEsperadas();

		long getAsistidas();

	}

	interface FilaCobertura {

		long getActivos();

		long getEnGrupo();

	}

}
