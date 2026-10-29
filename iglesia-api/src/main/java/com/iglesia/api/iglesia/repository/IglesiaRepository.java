package com.iglesia.api.iglesia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.iglesia.api.iglesia.entity.Iglesia;

public interface IglesiaRepository extends JpaRepository<Iglesia, Long>, JpaSpecificationExecutor<Iglesia> {

	boolean existsByNombreComercialIgnoreCase(String nombreComercial);

	boolean existsByNombreComercialIgnoreCaseAndIdNot(String nombreComercial, Long id);

}
