package com.iglesia.api.asistencia.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.asistencia.entity.Reunion;

/**
 * Reuniones de la iglesia del token. Las consultas JPQL las filtra Hibernate por {@code iglesia_id}; las nativas lo
 * hacen explícitamente (y la RLS lo garantiza).
 */
public interface ReunionRepository extends JpaRepository<Reunion, Long>, JpaSpecificationExecutor<Reunion> {

	/**
	 * Crea la reunión del culto en esa fecha si aún no existe. Con {@code ON CONFLICT DO NOTHING} dos equipos que la
	 * abren a la vez no fallan: uno la crea (devuelve 1) y el otro encuentra la misma (devuelve 0).
	 */
	@Modifying
	@Query(value = """
			INSERT INTO reunion (iglesia_id, tipo, nombre, fecha, hora, culto_programado_id, creada_por, creada_en)
			VALUES (:iglesiaId, 'CULTO', :nombre, :fecha, CAST(:hora AS time), :cultoId, :usuarioId, :ahora)
			ON CONFLICT (iglesia_id, culto_programado_id, fecha) WHERE culto_programado_id IS NOT NULL DO NOTHING""",
			nativeQuery = true)
	int abrirDeCulto(@Param("iglesiaId") long iglesiaId, @Param("cultoId") long cultoId, @Param("nombre") String nombre,
			@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora, @Param("usuarioId") long usuarioId,
			@Param("ahora") Instant ahora);

	/** Igual que {@link #abrirDeCulto} para la reunión de un grupo. */
	@Modifying
	@Query(value = """
			INSERT INTO reunion (iglesia_id, tipo, nombre, fecha, hora, grupo_id, creada_por, creada_en)
			VALUES (:iglesiaId, 'GRUPO', :nombre, :fecha, CAST(:hora AS time), :grupoId, :usuarioId, :ahora)
			ON CONFLICT (iglesia_id, grupo_id, fecha) WHERE grupo_id IS NOT NULL DO NOTHING""", nativeQuery = true)
	int abrirDeGrupo(@Param("iglesiaId") long iglesiaId, @Param("grupoId") long grupoId, @Param("nombre") String nombre,
			@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora, @Param("usuarioId") long usuarioId,
			@Param("ahora") Instant ahora);

	Optional<Reunion> findByCultoProgramadoIdAndFecha(Long cultoProgramadoId, LocalDate fecha);

	Optional<Reunion> findByGrupoIdAndFecha(Long grupoId, LocalDate fecha);

	List<Reunion> findByFecha(LocalDate fecha);

	/** Últimas reuniones a las que asistió el miembro, de cualquier tipo. */
	@Query("""
			select r from Reunion r join Asistencia a on a.reunionId = r.id
			where a.miembroId = :miembroId
			order by r.fecha desc, r.hora desc nulls last, r.id desc""")
	List<Reunion> ultimasAsistidas(@Param("miembroId") Long miembroId, Pageable limite);

}
