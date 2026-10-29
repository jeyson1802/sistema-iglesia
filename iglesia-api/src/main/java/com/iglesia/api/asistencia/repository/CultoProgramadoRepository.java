package com.iglesia.api.asistencia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.asistencia.entity.CultoProgramado;
import com.iglesia.api.grupos.model.DiaSemana;

/** Horarios de culto de la iglesia del token: Hibernate agrega el filtro por {@code iglesia_id}. */
public interface CultoProgramadoRepository extends JpaRepository<CultoProgramado, Long> {

	List<CultoProgramado> findByDiaSemanaAndActivoTrue(DiaSemana diaSemana);

}
