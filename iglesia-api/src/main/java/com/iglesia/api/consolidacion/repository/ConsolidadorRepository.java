package com.iglesia.api.consolidacion.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.consolidacion.entity.Consolidador;

public interface ConsolidadorRepository extends JpaRepository<Consolidador, Long> {

	Optional<Consolidador> findByMiembroId(Long miembroId);

	List<Consolidador> findByMiembroIdIn(Collection<Long> miembroIds);

	List<Consolidador> findByActivoTrue();

}
