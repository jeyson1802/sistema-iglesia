package com.iglesia.api.aportes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.iglesia.api.aportes.entity.CategoriaAporte;

/** Categorías de la iglesia del token: Hibernate agrega el filtro por {@code iglesia_id} a todas las consultas. */
public interface CategoriaAporteRepository extends JpaRepository<CategoriaAporte, Long> {

	List<CategoriaAporte> findAllByOrderByOrdenAscIdAsc();

	@Query("select coalesce(max(c.orden), 0) from CategoriaAporte c")
	int ultimoOrden();

}
