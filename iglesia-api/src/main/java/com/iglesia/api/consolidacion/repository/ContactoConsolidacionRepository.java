package com.iglesia.api.consolidacion.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.model.ConteoPorMiembro;

public interface ContactoConsolidacionRepository extends JpaRepository<ContactoConsolidacion, Long> {

	List<ContactoConsolidacion> findByCasoIdOrderByFechaHoraDescIdDesc(Long casoId);

	List<ContactoConsolidacion> findByCasoIdInOrderByFechaHoraDescIdDesc(Collection<Long> casoIds);

	Optional<ContactoConsolidacion> findFirstByCasoIdOrderByFechaHoraDescIdDesc(Long casoId);

	/** Contactos de todos los casos de una ronda (para su avance). */
	@Query("""
			select c from ContactoConsolidacion c
			where c.casoId in (select k.id from CasoConsolidacion k where k.rondaId = :rondaId)""")
	List<ContactoConsolidacion> deRonda(@Param("rondaId") Long rondaId);

	@Query("""
			select new com.iglesia.api.consolidacion.model.ConteoPorMiembro(c.consolidadorId, count(c))
			from ContactoConsolidacion c
			where c.consolidadorId in :miembroIds and c.fechaHora >= :desde
			group by c.consolidadorId""")
	List<ConteoPorMiembro> recientesPorConsolidador(@Param("miembroIds") Collection<Long> miembroIds,
			@Param("desde") Instant desde);

}
