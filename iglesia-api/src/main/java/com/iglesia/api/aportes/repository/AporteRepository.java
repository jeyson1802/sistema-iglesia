package com.iglesia.api.aportes.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.iglesia.api.aportes.entity.Aporte;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.aportes.model.TotalPorCategoria;
import com.iglesia.api.aportes.model.TotalPorMedio;

/**
 * Aportes de la iglesia del token (Hibernate filtra por {@code iglesia_id}). Los totales se agrupan en la base y no
 * cuentan los anulados; un filtro nulo no restringe.
 */
public interface AporteRepository extends JpaRepository<Aporte, Long>, JpaSpecificationExecutor<Aporte> {

	String FILTROS_VIGENTES = """
			a.anulado = false and a.fecha between :desde and :hasta
			and (:categoriaId is null or a.categoria.id = :categoriaId)
			and (:medio is null or a.medio = :medio)
			and (:miembroId is null or a.miembroId = :miembroId)""";

	@Override
	@EntityGraph(attributePaths = "categoria")
	Page<Aporte> findAll(Specification<Aporte> criterio, Pageable paginacion);

	@EntityGraph(attributePaths = "categoria")
	Optional<Aporte> findWithCategoriaById(Long id);

	boolean existsByAnuladoFalse();

	@Query("select new com.iglesia.api.aportes.model.TotalPorCategoria(a.categoria.id, sum(a.monto), count(a)) "
			+ "from Aporte a where " + FILTROS_VIGENTES + " group by a.categoria.id")
	List<TotalPorCategoria> totalesPorCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("categoriaId") Long categoriaId, @Param("medio") MedioAporte medio,
			@Param("miembroId") Long miembroId);

	@Query("select new com.iglesia.api.aportes.model.TotalPorMedio(a.medio, sum(a.monto), count(a)) "
			+ "from Aporte a where " + FILTROS_VIGENTES + " group by a.medio")
	List<TotalPorMedio> totalesPorMedio(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("categoriaId") Long categoriaId, @Param("medio") MedioAporte medio,
			@Param("miembroId") Long miembroId);

	@Query("select coalesce(sum(a.monto), 0) from Aporte a where " + FILTROS_VIGENTES)
	BigDecimal total(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("categoriaId") Long categoriaId, @Param("medio") MedioAporte medio,
			@Param("miembroId") Long miembroId);

}
