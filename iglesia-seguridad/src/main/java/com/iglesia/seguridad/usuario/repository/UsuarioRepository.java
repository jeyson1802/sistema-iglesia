package com.iglesia.seguridad.usuario.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = { "roles", "roles.rolBase", "roles.permisos" })
	Optional<Usuario> findByCorreo(String correo);

	@EntityGraph(attributePaths = { "roles", "roles.rolBase", "roles.permisos" })
	Optional<Usuario> findConRolesById(Long id);

	Page<Usuario> findByIglesiaId(Long iglesiaId, Pageable paginacion);

	boolean existsByCorreo(String correo);

	boolean existsByIglesiaIdAndMiembroId(Long iglesiaId, Long miembroId);

	boolean existsByIglesiaIdAndMiembroIdAndIdNot(Long iglesiaId, Long miembroId, Long id);

	@Query("select count(u) > 0 from Usuario u join u.roles r where r.codigo = :rol")
	boolean existeConRol(@Param("rol") RolBase rol);

}
