package com.iglesia.api.ubigeo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.ubigeo.entity.Provincia;

public interface ProvinciaRepository extends JpaRepository<Provincia, String> {

	List<Provincia> findByDepartamentoCodigoOrderByNombreAsc(String departamentoCodigo);

}
