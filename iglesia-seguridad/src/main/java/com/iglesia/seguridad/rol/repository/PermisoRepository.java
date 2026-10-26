package com.iglesia.seguridad.rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.seguridad.rol.entity.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, String> {

}
