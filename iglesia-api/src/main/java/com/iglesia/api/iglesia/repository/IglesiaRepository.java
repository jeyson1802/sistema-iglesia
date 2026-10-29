package com.iglesia.api.iglesia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.iglesia.entity.Iglesia;

public interface IglesiaRepository extends JpaRepository<Iglesia, Long> {

	List<Iglesia> findAllByOrderByNombreAsc();

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

}
