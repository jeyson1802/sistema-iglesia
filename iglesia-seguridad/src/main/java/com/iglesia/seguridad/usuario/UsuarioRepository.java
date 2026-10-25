package com.iglesia.seguridad.usuario;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findByCorreo(String correo);

	@EntityGraph(attributePaths = "roles")
	Optional<Usuario> findConRolesById(Long id);

	Page<Usuario> findByIglesiaId(Long iglesiaId, Pageable paginacion);

	boolean existsByCorreo(String correo);

	boolean existsByIglesiaIdAndMiembroId(Long iglesiaId, Long miembroId);

	boolean existsByIglesiaIdAndMiembroIdAndIdNot(Long iglesiaId, Long miembroId, Long id);

	@Query("select count(u) > 0 from Usuario u join u.roles r where r = :rol")
	boolean existeConRol(@Param("rol") Rol rol);

}
