package com.iglesia.seguridad.actividad.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.seguridad.actividad.entity.ActividadUsuario;
import com.iglesia.seguridad.actividad.entity.ActividadUsuarioId;

/**
 * Actividad diaria de los usuarios. Las consultas de estadísticas reciben {@code iglesiaId} nulo para contar todas
 * las iglesias (administrador del sistema) o una iglesia concreta (administrador de esa iglesia).
 */
public interface ActividadUsuarioRepository extends JpaRepository<ActividadUsuario, ActividadUsuarioId> {

	/** Registra la actividad del día; si ya había una fila de ese usuario ese día, no hace nada. */
	@Modifying
	@Query(value = """
			INSERT INTO actividad_usuario (usuario_id, fecha, iglesia_id, rol_base)
			VALUES (:usuarioId, :fecha, :iglesiaId, :rolBase)
			ON CONFLICT (usuario_id, fecha) DO NOTHING""", nativeQuery = true)
	void registrar(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDate fecha,
			@Param("iglesiaId") Long iglesiaId, @Param("rolBase") String rolBase);

	@Modifying
	@Query("delete from ActividadUsuario a where a.id.fecha < :limite")
	int borrarAnterioresA(@Param("limite") LocalDate limite);

	/** Usuarios distintos con actividad entre las fechas (inclusivas). */
	@Query(value = """
			SELECT count(DISTINCT usuario_id) FROM actividad_usuario
			WHERE fecha BETWEEN :desde AND :hasta
			  AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)""", nativeQuery = true)
	long usuariosActivos(@Param("iglesiaId") Long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	/** Suma de usuarios activos de cada día con actividad: dividida por los días, da el promedio diario. */
	@Query(value = """
			SELECT count(*) FROM actividad_usuario
			WHERE fecha BETWEEN :desde AND :hasta
			  AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)""", nativeQuery = true)
	long diasUsuario(@Param("iglesiaId") Long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	/** Iglesias distintas con al menos un usuario activo (sin contar al administrador del sistema). */
	@Query(value = """
			SELECT count(DISTINCT iglesia_id) FROM actividad_usuario
			WHERE fecha BETWEEN :desde AND :hasta AND iglesia_id IS NOT NULL""", nativeQuery = true)
	long iglesiasActivas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	/** Usuarios distintos por día, semana o mes ({@code unidad} es {@code day}, {@code week} o {@code month}). */
	@Query(value = """
			SELECT CAST(date_trunc(:unidad, fecha) AS date) AS inicio, count(DISTINCT usuario_id) AS valor
			FROM actividad_usuario
			WHERE fecha BETWEEN :desde AND :hasta
			  AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)
			GROUP BY 1 ORDER BY 1""", nativeQuery = true)
	List<FilaSerie> serie(@Param("iglesiaId") Long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta, @Param("unidad") String unidad);

	/** Usuarios distintos por rol base. */
	@Query(value = """
			SELECT rol_base AS clave, count(DISTINCT usuario_id) AS valor FROM actividad_usuario
			WHERE fecha BETWEEN :desde AND :hasta
			  AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)
			GROUP BY rol_base""", nativeQuery = true)
	List<FilaConteo> porRolBase(@Param("iglesiaId") Long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	/** Cuentas habilitadas (de una iglesia o de todas). */
	@Query(value = """
			SELECT count(*) FROM usuario
			WHERE activo AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)""", nativeQuery = true)
	long usuariosHabilitados(@Param("iglesiaId") Long iglesiaId);

	/** Usuarios creados entre las fechas (inclusivas). */
	@Query(value = """
			SELECT count(*) FROM usuario
			WHERE CAST(creado_en AS date) BETWEEN :desde AND :hasta
			  AND (CAST(:iglesiaId AS bigint) IS NULL OR iglesia_id = :iglesiaId)""", nativeQuery = true)
	long usuariosCreados(@Param("iglesiaId") Long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	interface FilaSerie {

		LocalDate getInicio();

		long getValor();

	}

	interface FilaConteo {

		String getClave();

		long getValor();

	}

}
