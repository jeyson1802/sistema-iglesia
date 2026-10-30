package com.iglesia.api.miembros.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.ConteoPorGrupo;
import com.iglesia.api.miembros.model.ConteoPorLider;

/**
 * Miembros de la iglesia del token. Las consultas JPQL las filtra Hibernate por {@code iglesia_id}; las CTE
 * recursivas son nativas y filtran {@code iglesia_id} explícitamente, también en el paso recursivo.
 */
public interface MiembroRepository extends JpaRepository<Miembro, Long>, JpaSpecificationExecutor<Miembro> {

	/** Listado con nivel y líder en la misma consulta (evita N+1). */
	@Override
	@EntityGraph(attributePaths = { "nivel", "lider" })
	Page<Miembro> findAll(Specification<Miembro> criterio, Pageable paginacion);

	/** Ficha completa con todas sus referencias en una sola consulta. */
	@EntityGraph(attributePaths = { "nivel", "lider", "invitadoPor", "registradoPor" })
	Optional<Miembro> findWithReferenciasById(Long id);

	Optional<Miembro> findByPastorPrincipalTrue();

	@EntityGraph(attributePaths = { "nivel" })
	Optional<Miembro> findByCodigoAsistencia(String codigoAsistencia);

	boolean existsByCodigoAsistencia(String codigoAsistencia);

	@EntityGraph(attributePaths = { "nivel" })
	List<Miembro> findWithNivelByIdIn(Collection<Long> ids);

	long countByLiderIdAndActivoTrue(Long liderId);

	boolean existsByGrupoIdAndActivoTrue(Long grupoId);

	/** Liderados directos activos, con su nivel, para expandir un nodo del árbol de liderazgo. */
	@EntityGraph(attributePaths = { "nivel" })
	List<Miembro> findByLiderIdAndActivoTrueOrderByApellidosAscNombresAscIdAsc(Long liderId);

	/** Integrantes activos de un grupo, con su nivel. */
	@EntityGraph(attributePaths = { "nivel" })
	List<Miembro> findByGrupoIdAndActivoTrueOrderByApellidosAscNombresAscIdAsc(Long grupoId);

	@Query("""
			select new com.iglesia.api.miembros.model.ConteoPorLider(m.lider.id, count(m))
			from Miembro m
			where m.activo = true and m.lider.id in :liderIds
			group by m.lider.id""")
	List<ConteoPorLider> contarActivosPorLider(@Param("liderIds") Collection<Long> liderIds);

	/**
	 * Candidatos a duplicado: nombres y apellidos de la misma longitud (los textos se guardan normalizados). La
	 * comparación sin distinguir mayúsculas se termina en el servicio, porque {@code lower()} de PostgreSQL depende
	 * de la configuración regional del servidor.
	 */
	@EntityGraph(attributePaths = { "nivel", "lider" })
	/**
	 * Miembros activos fuera del grupo, los últimos en llegar a la iglesia primero (por fecha de ingreso o, si no
	 * tienen, por la de registro).
	 */
	@Query("""
			select m from Miembro m
			where m.activo = true and (m.grupoId is null or m.grupoId <> :grupoId)
			order by coalesce(m.fechaIngreso, cast(m.creadoEn as LocalDate)) desc, m.id desc""")
	List<Miembro> recientesFueraDelGrupo(@Param("grupoId") Long grupoId, Pageable paginacion);

	@Query("""
			select m from Miembro m
			where length(m.nombres) = :largoNombres and length(m.apellidos) = :largoApellidos
			order by m.id""")
	List<Miembro> buscarPorLargoDeNombre(@Param("largoNombres") int largoNombres,
			@Param("largoApellidos") int largoApellidos);

	@Query("""
			select new com.iglesia.api.miembros.model.ConteoPorGrupo(m.grupoId, count(m))
			from Miembro m
			where m.activo = true and m.grupoId in :grupoIds
			group by m.grupoId""")
	List<ConteoPorGrupo> contarActivosPorGrupo(@Param("grupoIds") Collection<Long> grupoIds);

	/** Ids de toda la red del miembro (a quienes lidera directa e indirectamente), sin incluirlo. */
	@Query(value = """
			WITH RECURSIVE red (id) AS (
			    SELECT m.id FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.lider_id = :raizId
			    UNION
			    SELECT m.id FROM miembro m JOIN red r ON m.lider_id = r.id
			    WHERE m.iglesia_id = :iglesiaId)
			SELECT id FROM red""", nativeQuery = true)
	List<Long> idsDeLaRed(@Param("iglesiaId") long iglesiaId, @Param("raizId") long raizId);

	/** Si {@code candidatoId} pertenece a la red de {@code raizId}: asignarlo como líder de la raíz sería un ciclo. */
	@Query(value = """
			SELECT EXISTS (
			    WITH RECURSIVE red (id) AS (
			        SELECT m.id FROM miembro m
			        WHERE m.iglesia_id = :iglesiaId AND m.lider_id = :raizId
			        UNION
			        SELECT m.id FROM miembro m JOIN red r ON m.lider_id = r.id
			        WHERE m.iglesia_id = :iglesiaId)
			    SELECT 1 FROM red WHERE id = :candidatoId)""", nativeQuery = true)
	boolean esDeLaRed(@Param("iglesiaId") long iglesiaId, @Param("raizId") long raizId,
			@Param("candidatoId") long candidatoId);

	/** Ids de la cadena de liderazgo, del líder directo al pastor. {@code CYCLE} corta un ciclo si lo hubiera. */
	@Query(value = """
			WITH RECURSIVE cadena (id, paso) AS (
			    SELECT m.lider_id, 1 FROM miembro m
			    WHERE m.iglesia_id = :iglesiaId AND m.id = :miembroId AND m.lider_id IS NOT NULL
			    UNION ALL
			    SELECT m.lider_id, c.paso + 1 FROM miembro m JOIN cadena c ON m.id = c.id
			    WHERE m.iglesia_id = :iglesiaId AND m.lider_id IS NOT NULL
			) CYCLE id SET en_ciclo USING ruta
			SELECT id FROM cadena WHERE NOT en_ciclo ORDER BY paso""", nativeQuery = true)
	List<Long> idsDeLaCadena(@Param("iglesiaId") long iglesiaId, @Param("miembroId") long miembroId);

}
