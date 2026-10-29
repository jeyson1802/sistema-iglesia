package com.iglesia.api.grupos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.grupos.entity.Grupo;

/** Grupos de la iglesia del token: Hibernate agrega el filtro por {@code iglesia_id}. */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

	boolean existsByLiderId(Long liderId);

	boolean existsByLiderIdAndIdNot(Long liderId, Long id);

	boolean existsByLiderIdAndActivoTrue(Long liderId);

}
