package com.iglesia.api.asistencia.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.asistencia.entity.Asistencia;
import com.iglesia.api.asistencia.entity.AsistenciaId;
import com.iglesia.api.asistencia.model.FechaAsistida;
import com.iglesia.api.asistencia.model.TotalPorReunion;

/**
 * Asistencias de la iglesia del token. Las consultas JPQL las filtra Hibernate por {@code iglesia_id}; la inserción
 * nativa lo hace explícitamente (y la RLS lo garantiza).
 */
public interface AsistenciaRepository extends JpaRepository<Asistencia, AsistenciaId> {

	/**
	 * Marca al miembro en la reunión si aún no estaba. Devuelve 1 si lo marcó y 0 si ya estaba (aunque lo haya marcado
	 * otro equipo en el mismo instante): nunca falla por duplicado.
	 */
	@Modifying
	@Query(value = """
			INSERT INTO asistencia (iglesia_id, reunion_id, miembro_id, medio, registrada_por, registrada_en)
			VALUES (:iglesiaId, :reunionId, :miembroId, :medio, :usuarioId, :ahora)
			ON CONFLICT DO NOTHING""", nativeQuery = true)
	int marcar(@Param("iglesiaId") long iglesiaId, @Param("reunionId") long reunionId,
			@Param("miembroId") long miembroId, @Param("medio") String medio, @Param("usuarioId") long usuarioId,
			@Param("ahora") Instant ahora);

	Optional<Asistencia> findByReunionIdAndMiembroId(Long reunionId, Long miembroId);

	long countByReunionId(Long reunionId);

	boolean existsByReunionId(Long reunionId);

	@Query("""
			select new com.iglesia.api.asistencia.model.TotalPorReunion(a.reunionId, count(a))
			from Asistencia a where a.reunionId in :reunionIds group by a.reunionId""")
	List<TotalPorReunion> contarPorReunion(@Param("reunionIds") Collection<Long> reunionIds);

	Page<Asistencia> findByReunionId(Long reunionId, Pageable paginacion);

	Page<Asistencia> findByReunionIdAndMiembroIdIn(Long reunionId, Collection<Long> miembroIds, Pageable paginacion);

	@Query("select a.miembroId from Asistencia a where a.reunionId = :reunionId")
	List<Long> miembrosDe(@Param("reunionId") Long reunionId);

	@Query("select a.miembroId from Asistencia a where a.reunionId = :reunionId and a.miembroId in :miembroIds")
	List<Long> presentesEntre(@Param("reunionId") Long reunionId, @Param("miembroIds") Collection<Long> miembroIds);

	/** Fechas de las reuniones a las que asistieron los miembros desde una fecha, en una sola consulta. */
	@Query("""
			select new com.iglesia.api.asistencia.model.FechaAsistida(a.miembroId, r.fecha)
			from Asistencia a join Reunion r on r.id = a.reunionId
			where a.miembroId in :miembroIds and r.fecha >= :desde""")
	List<FechaAsistida> fechasAsistidas(@Param("miembroIds") Collection<Long> miembroIds,
			@Param("desde") LocalDate desde);

}
