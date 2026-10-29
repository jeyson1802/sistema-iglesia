package com.iglesia.api.niveles.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.iglesia.api.niveles.entity.Nivel;

/** Niveles de la iglesia del token: Hibernate agrega el filtro por {@code iglesia_id} a todas las consultas. */
public interface NivelRepository extends JpaRepository<Nivel, Long> {

	List<Nivel> findAllByOrderByOrdenAscIdAsc();

	@Query("select coalesce(max(n.orden), 0) from Nivel n")
	int ultimoOrden();

}
