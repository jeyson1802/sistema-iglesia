package com.iglesia.api.grupos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.model.DiaSemana;

/** Grupos de la iglesia del token: Hibernate agrega el filtro por {@code iglesia_id}. */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

	boolean existsByLiderId(Long liderId);

	boolean existsByLiderIdAndIdNot(Long liderId, Long id);

	boolean existsByLiderIdAndActivoTrue(Long liderId);

	List<Grupo> findByDiaReunionAndActivoTrue(DiaSemana diaReunion);

	List<Grupo> findByLiderId(Long liderId);

}
