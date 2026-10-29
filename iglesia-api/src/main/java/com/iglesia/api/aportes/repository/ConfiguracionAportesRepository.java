package com.iglesia.api.aportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.api.aportes.entity.ConfiguracionAportes;

public interface ConfiguracionAportesRepository extends JpaRepository<ConfiguracionAportes, Long> {
}
