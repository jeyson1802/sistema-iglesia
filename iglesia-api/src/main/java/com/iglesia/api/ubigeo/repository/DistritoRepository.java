package com.iglesia.api.ubigeo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.ubigeo.entity.Distrito;

public interface DistritoRepository extends JpaRepository<Distrito, String> {

	List<Distrito> findByProvinciaCodigoOrderByNombreAsc(String provinciaCodigo);

	/** Distrito con su provincia y su departamento en una sola consulta. */
	@Query("""
			select d from Distrito d
			join fetch d.provincia p
			join fetch p.departamento
			where d.codigo = :codigo""")
	Optional<Distrito> buscarConUbicacion(@Param("codigo") String codigo);

	@Query("""
			select d from Distrito d
			join fetch d.provincia p
			join fetch p.departamento
			where d.codigo in :codigos""")
	List<Distrito> buscarConUbicacion(@Param("codigos") Collection<String> codigos);

}
