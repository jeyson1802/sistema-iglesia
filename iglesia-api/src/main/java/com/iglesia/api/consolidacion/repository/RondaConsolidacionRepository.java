package com.iglesia.api.consolidacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.iglesia.api.consolidacion.entity.RondaConsolidacion;

public interface RondaConsolidacionRepository
		extends JpaRepository<RondaConsolidacion, Long>, JpaSpecificationExecutor<RondaConsolidacion> {

}
