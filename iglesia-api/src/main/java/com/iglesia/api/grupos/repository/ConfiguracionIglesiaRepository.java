package com.iglesia.api.grupos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.grupos.entity.ConfiguracionIglesia;

public interface ConfiguracionIglesiaRepository extends JpaRepository<ConfiguracionIglesia, Long> {
}
