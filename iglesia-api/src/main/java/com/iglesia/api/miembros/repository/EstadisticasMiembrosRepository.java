package com.iglesia.api.miembros.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.common.estadisticas.constant.ConsultasAlcance;
import com.iglesia.api.miembros.entity.Miembro;

/**
 * Cifras agregadas de los miembros de la iglesia o de la red de un líder ({@code liderId} nulo: toda la iglesia).
 * Las bajas son los miembros inactivos cuya última modificación cae en el rango (la desactivación no guarda fecha
 * propia).
 */
public interface EstadisticasMiembrosRepository extends Repository<Miembro, Long> {

	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT count(*) FILTER (WHERE activo) AS activos,
			       count(*) FILTER (WHERE COALESCE(fecha_ingreso, CAST(creado_en AS date)) BETWEEN :desde AND :hasta) AS altas,
			       count(*) FILTER (WHERE COALESCE(fecha_ingreso, CAST(creado_en AS date))
			                        BETWEEN :anteriorDesde AND :anteriorHasta) AS altasAnterior,
			       count(*) FILTER (WHERE NOT activo AND CAST(actualizado_en AS date) BETWEEN :desde AND :hasta) AS bajas,
			       count(*) FILTER (WHERE NOT activo AND CAST(actualizado_en AS date)
			                        BETWEEN :anteriorDesde AND :anteriorHasta) AS bajasAnterior,
			       count(*) FILTER (WHERE activo AND grupo_id IS NULL) AS sinGrupo,
			       count(*) FILTER (WHERE activo AND lider_id IS NULL AND NOT es_pastor_principal) AS sinLider,
			       count(*) FILTER (WHERE activo AND lider_id = CAST(:liderId AS bigint)) AS directos
			FROM alcance""", nativeQuery = true)
	FilaResumen resumen(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("anteriorDesde") LocalDate anteriorDesde, @Param("anteriorHasta") LocalDate anteriorHasta);

	/** Altas y bajas por día, semana o mes ({@code unidad}: {@code day}, {@code week} o {@code month}). */
	@Query(value = ConsultasAlcance.ALCANCE + """
			, eventos AS (
			    SELECT COALESCE(fecha_ingreso, CAST(creado_en AS date)) AS fecha, 1 AS alta, 0 AS baja FROM alcance
			    UNION ALL
			    SELECT CAST(actualizado_en AS date), 0, 1 FROM alcance WHERE NOT activo
			)
			SELECT CAST(date_trunc(:unidad, CAST(fecha AS timestamp)) AS date) AS inicio,
			       sum(alta) AS altas, sum(baja) AS bajas
			FROM eventos
			WHERE fecha BETWEEN :desde AND :hasta
			GROUP BY 1 ORDER BY 1""", nativeQuery = true)
	List<FilaSerie> serie(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("unidad") String unidad);

	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT COALESCE(condicion, 'SIN_DATO') AS clave, CAST(NULL AS varchar) AS etiqueta, count(*) AS valor
			FROM alcance WHERE activo GROUP BY 1""", nativeQuery = true)
	List<FilaConteo> porCondicion(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId);

	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT COALESCE(genero, 'SIN_DATO') AS clave, CAST(NULL AS varchar) AS etiqueta, count(*) AS valor
			FROM alcance WHERE activo GROUP BY 1""", nativeQuery = true)
	List<FilaConteo> porGenero(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId);

	/** Niveles en su orden; los miembros sin nivel van con clave {@code SIN_NIVEL}. */
	@Query(value = ConsultasAlcance.ALCANCE + """
			SELECT COALESCE(CAST(n.id AS varchar), 'SIN_NIVEL') AS clave, n.nombre AS etiqueta, count(*) AS valor
			FROM alcance a LEFT JOIN nivel n ON n.id = a.nivel_id AND n.iglesia_id = :iglesiaId
			WHERE a.activo
			GROUP BY n.id, n.nombre, n.orden
			ORDER BY n.orden NULLS LAST""", nativeQuery = true)
	List<FilaConteo> porNivel(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId);

	/** Edad cumplida a {@code hoy}, por la fecha de nacimiento o, si no la tiene, por el año estimado. */
	@Query(value = ConsultasAlcance.ALCANCE + """
			, edades AS (
			    SELECT COALESCE(CAST(date_part('year', age(CAST(:hoy AS date), fecha_nacimiento)) AS int),
			                    CAST(date_part('year', CAST(:hoy AS date)) AS int) - anio_nacimiento_estimado) AS edad
			    FROM alcance WHERE activo
			)
			SELECT CASE
			           WHEN edad IS NULL THEN 'SIN_DATO'
			           WHEN edad <= 12 THEN 'NINOS'
			           WHEN edad <= 17 THEN 'ADOLESCENTES'
			           WHEN edad <= 29 THEN 'JOVENES'
			           WHEN edad <= 44 THEN 'ADULTOS_JOVENES'
			           WHEN edad <= 59 THEN 'ADULTOS'
			           ELSE 'MAYORES'
			       END AS clave, CAST(NULL AS varchar) AS etiqueta, count(*) AS valor
			FROM edades GROUP BY 1""", nativeQuery = true)
	List<FilaConteo> porEdad(@Param("iglesiaId") long iglesiaId, @Param("liderId") Long liderId,
			@Param("hoy") LocalDate hoy);

	interface FilaResumen {

		long getActivos();

		long getAltas();

		long getAltasAnterior();

		long getBajas();

		long getBajasAnterior();

		long getSinGrupo();

		long getSinLider();

		long getDirectos();

	}

	interface FilaSerie {

		LocalDate getInicio();

		long getAltas();

		long getBajas();

	}

	interface FilaConteo {

		String getClave();

		String getEtiqueta();

		long getValor();

	}

}
