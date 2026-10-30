package com.iglesia.api.consolidacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;

public interface ResultadoConsolidacionRepository extends JpaRepository<ResultadoConsolidacion, Long> {

	List<ResultadoConsolidacion> findAllByOrderByOrdenAscIdAsc();

	@Query("select coalesce(max(r.orden), 0) from ResultadoConsolidacion r")
	int ordenMaximo();

}
