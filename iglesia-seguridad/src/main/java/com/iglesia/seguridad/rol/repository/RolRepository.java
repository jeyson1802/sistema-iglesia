package com.iglesia.seguridad.rol.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.CantidadUsuariosRol;
import com.iglesia.seguridad.rol.model.RolBase;

/**
 * Las consultas sobre {@code usuario_rol} son nativas para que el contexto de roles no dependa de la
 * entidad {@code Usuario} (que ya depende de {@code Rol}).
 */
public interface RolRepository extends JpaRepository<Rol, Long> {

	List<Rol> findByCodigoIn(Collection<RolBase> codigos);

	@EntityGraph(attributePaths = "rolBase")
	List<Rol> findByIdIn(Collection<Long> ids);

	@EntityGraph(attributePaths = { "rolBase", "rolBase.permisos", "permisos" })
	Optional<Rol> findConPermisosById(Long id);

	/** Roles base más los personalizados de la iglesia; sin iglesia (nulo), solo los base. */
	@EntityGraph(attributePaths = { "rolBase", "permisos" })
	List<Rol> findByIglesiaIdIsNullOrIglesiaId(Long iglesiaId);

	boolean existsByIglesiaIdAndNombreIgnoreCase(Long iglesiaId, String nombre);

	boolean existsByIglesiaIdAndNombreIgnoreCaseAndIdNot(Long iglesiaId, String nombre, Long id);

	/** Usuarios por rol, contando solo los de esa iglesia; sin iglesia (nulo), los de todas. */
	@Query(value = """
			SELECT ur.rol_id AS rolId, count(*) AS cantidad
			FROM usuario_rol ur
			JOIN usuario u ON u.id = ur.usuario_id
			WHERE ur.rol_id IN (:rolIds)
			  AND (CAST(:iglesiaId AS BIGINT) IS NULL OR u.iglesia_id = :iglesiaId)
			GROUP BY ur.rol_id
			""", nativeQuery = true)
	List<CantidadUsuariosRol> contarUsuarios(@Param("rolIds") Collection<Long> rolIds,
			@Param("iglesiaId") Long iglesiaId);

	/** Si algún usuario con ese rol se quedaría sin ningún otro rol activo. */
	@Query(value = """
			SELECT EXISTS (
			    SELECT 1 FROM usuario_rol ur
			    WHERE ur.rol_id = :rolId
			      AND NOT EXISTS (
			          SELECT 1 FROM usuario_rol otro
			          JOIN rol r ON r.id = otro.rol_id
			          WHERE otro.usuario_id = ur.usuario_id AND otro.rol_id <> :rolId AND r.activo))
			""", nativeQuery = true)
	boolean dejariaUsuariosSinRolesActivos(@Param("rolId") Long rolId);

	/** Bloqueo transaccional por iglesia; PostgreSQL lo libera solo al terminar la transacción. */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:iglesiaId)", nativeQuery = true)
	Integer bloquearAdministracionDeIglesia(@Param("iglesiaId") Long iglesiaId);

	/** Usuarios activos de la iglesia con algún rol activo que tiene ese permiso. */
	@Query(value = """
			SELECT count(DISTINCT u.id)
			FROM usuario u
			JOIN usuario_rol ur ON ur.usuario_id = u.id
			JOIN rol r ON r.id = ur.rol_id
			JOIN rol_permiso rp ON rp.rol_id = r.id
			WHERE u.iglesia_id = :iglesiaId
			  AND u.activo
			  AND r.activo
			  AND rp.permiso_codigo = :permiso
			""", nativeQuery = true)
	long contarAdministradoresActivos(@Param("iglesiaId") Long iglesiaId, @Param("permiso") String permiso);

}
