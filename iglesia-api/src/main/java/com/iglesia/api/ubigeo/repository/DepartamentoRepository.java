package com.iglesia.api.ubigeo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.ubigeo.entity.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {

	List<Departamento> findAllByOrderByNombreAsc();

}
