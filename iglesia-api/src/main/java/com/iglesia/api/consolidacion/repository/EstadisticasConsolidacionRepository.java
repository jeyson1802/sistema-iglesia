package com.iglesia.api.consolidacion.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.consolidacion.entity.CasoConsolidacion;

/** Cifras agregadas de la consolidación de toda la iglesia. Los contactos cuentan por el día en que se hicieron. */
public interface EstadisticasConsolidacionRepository extends Repository<CasoConsolidacion, Long> {

	@Query(value = """
			SELECT (SELECT count(*) FROM ronda_consolidacion
			        WHERE iglesia_id = :iglesiaId AND estado = 'EN_CURSO') AS rondasEnCurso,
			       (SELECT count(*) FROM caso_consolidacion
			        WHERE iglesia_id = :iglesiaId AND estado <> 'CERRADO') AS casosAbiertos,
			       (SELECT count(*) FROM caso_consolidacion
			        WHERE iglesia_id = :iglesiaId AND CAST(cerrado_en AS date) BETWEEN :desde AND :hasta) AS cerrados,
			       (SELECT count(*) FROM caso_consolidacion
			        WHERE iglesia_id = :iglesiaId
			          AND CAST(cerrado_en AS date) BETWEEN :anteriorDesde AND :anteriorHasta) AS cerradosAnterior,
			       (SELECT count(*) FROM contacto_consolidacion
			        WHERE iglesia_id = :iglesiaId AND CAST(fecha_hora AS date) BETWEEN :desde AND :hasta) AS contactos,
			       (SELECT count(*) FROM contacto_consolidacion
			        WHERE iglesia_id = :iglesiaId
			          AND CAST(fecha_hora AS date) BETWEEN :anteriorDesde AND :anteriorHasta) AS contactosAnterior""",
			nativeQuery = true)
	FilaResumen resumen(@Param("iglesiaId") long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta, @Param("anteriorDesde") LocalDate anteriorDesde,
			@Param("anteriorHasta") LocalDate anteriorHasta);

	/** Personas contactadas en el rango y cuántas asistieron a alguna reunión después de su primer contacto. */
	@Query(value = """
			WITH primeros AS (
			    SELECT c.miembro_id, min(CAST(k.fecha_hora AS date)) AS primero
			    FROM contacto_consolidacion k
			    JOIN caso_consolidacion c ON c.id = k.caso_id AND c.iglesia_id = :iglesiaId
			    WHERE k.iglesia_id = :iglesiaId AND CAST(k.fecha_hora AS date) BETWEEN :desde AND :hasta
			    GROUP BY c.miembro_id
			)
			SELECT count(*) AS contactadas,
			       count(*) FILTER (WHERE EXISTS (
			           SELECT 1 FROM asistencia a JOIN reunion r ON r.id = a.reunion_id AND r.iglesia_id = :iglesiaId
			           WHERE a.iglesia_id = :iglesiaId AND a.miembro_id = p.miembro_id AND r.fecha > p.primero)) AS volvieron
			FROM primeros p""", nativeQuery = true)
	FilaRetorno retorno(@Param("iglesiaId") long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	@Query(value = """
			SELECT medio AS clave, count(*) AS valor FROM contacto_consolidacion
			WHERE iglesia_id = :iglesiaId AND CAST(fecha_hora AS date) BETWEEN :desde AND :hasta
			GROUP BY medio""", nativeQuery = true)
	List<FilaConteo> contactosPorMedio(@Param("iglesiaId") long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	/** Casos cerrados en el rango por resultado de la iteración, en el orden de los resultados. */
	@Query(value = """
			SELECT r.nombre AS clave, count(*) AS valor
			FROM caso_consolidacion c
			JOIN resultado_consolidacion r ON r.id = c.resultado_id AND r.iglesia_id = :iglesiaId
			WHERE c.iglesia_id = :iglesiaId AND CAST(c.cerrado_en AS date) BETWEEN :desde AND :hasta
			GROUP BY r.id, r.nombre, r.orden
			ORDER BY r.orden""", nativeQuery = true)
	List<FilaConteo> cierresPorResultado(@Param("iglesiaId") long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	interface FilaResumen {

		long getRondasEnCurso();

		long getCasosAbiertos();

		long getCerrados();

		long getCerradosAnterior();

		long getContactos();

		long getContactosAnterior();

	}

	interface FilaRetorno {

		long getContactadas();

		long getVolvieron();

	}

	interface FilaConteo {

		String getClave();

		long getValor();

	}

}
