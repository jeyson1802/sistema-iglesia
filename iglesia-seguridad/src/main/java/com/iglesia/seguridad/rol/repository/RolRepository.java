package com.iglesia.seguridad.rol.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.RolBase;

public interface RolRepository extends JpaRepository<Rol, Long> {

	List<Rol> findByCodigoIn(Collection<RolBase> codigos);

	@EntityGraph(attributePaths = "rolBase")
	List<Rol> findByIdIn(Collection<Long> ids);

}
