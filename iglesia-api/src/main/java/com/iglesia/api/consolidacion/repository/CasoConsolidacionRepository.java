package com.iglesia.api.consolidacion.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.model.ConteoPorMiembro;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.ResumenRonda;

public interface CasoConsolidacionRepository
		extends JpaRepository<CasoConsolidacion, Long>, JpaSpecificationExecutor<CasoConsolidacion> {

	/**
	 * Personas que entran en un lote: activas, de la iglesia, que aún no están en la ronda y que cumplen los filtros.
	 * Cada filtro nulo (o falso) no restringe. {@code ids} es una lista separada por comas que solo se aplica con
	 * {@code filtrarIds} (personas elegidas a mano o con una alerta de asistencia).
	 */
	String CANDIDATOS = """
			FROM miembro m
			WHERE m.iglesia_id = :iglesiaId AND m.activo
			  AND (CAST(:condicion AS varchar) IS NULL OR m.condicion = CAST(:condicion AS varchar))
			  AND (CAST(:ingresoDesde AS date) IS NULL
			       OR COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date)) >= CAST(:ingresoDesde AS date))
			  AND (CAST(:ingresoHasta AS date) IS NULL
			       OR COALESCE(m.fecha_ingreso, CAST(m.creado_en AS date)) <= CAST(:ingresoHasta AS date))
			  AND (CAST(:grupoId AS bigint) IS NULL OR m.grupo_id = CAST(:grupoId AS bigint))
			  AND (NOT CAST(:sinGrupo AS boolean) OR m.grupo_id IS NULL)
			  AND (NOT CAST(:nuncaConsolidados AS boolean) OR NOT EXISTS (
			       SELECT 1 FROM caso_consolidacion n WHERE n.iglesia_id = m.iglesia_id AND n.miembro_id = m.id))
			  AND (NOT CAST(:filtrarIds AS boolean) OR m.id = ANY (CAST(string_to_array(:ids, ',') AS bigint[])))
			  AND NOT EXISTS (SELECT 1 FROM caso_consolidacion r
			       WHERE r.iglesia_id = m.iglesia_id AND r.ronda_id = :rondaId AND r.miembro_id = m.id)
			""";

	/** Condición de «ya tiene un seguimiento abierto» (en otra ronda o individual). */
	String CON_CASO_ABIERTO = """
			EXISTS (SELECT 1 FROM caso_consolidacion a
			        WHERE a.iglesia_id = m.iglesia_id AND a.miembro_id = m.id AND a.estado <> 'CERRADO')""";

	@Query(value = "SELECT COUNT(*) FILTER (WHERE NOT " + CON_CASO_ABIERTO + ") " + CANDIDATOS, nativeQuery = true)
	long contarLote(@Param("iglesiaId") long iglesiaId, @Param("rondaId") long rondaId,
			@Param("condicion") String condicion, @Param("ingresoDesde") LocalDate ingresoDesde,
			@Param("ingresoHasta") LocalDate ingresoHasta, @Param("grupoId") Long grupoId,
			@Param("sinGrupo") boolean sinGrupo, @Param("nuncaConsolidados") boolean nuncaConsolidados,
			@Param("filtrarIds") boolean filtrarIds, @Param("ids") String ids);

	@Query(value = "SELECT COUNT(*) FILTER (WHERE " + CON_CASO_ABIERTO + ") " + CANDIDATOS, nativeQuery = true)
	long contarConCasoAbierto(@Param("iglesiaId") long iglesiaId, @Param("rondaId") long rondaId,
			@Param("condicion") String condicion, @Param("ingresoDesde") LocalDate ingresoDesde,
			@Param("ingresoHasta") LocalDate ingresoHasta, @Param("grupoId") Long grupoId,
			@Param("sinGrupo") boolean sinGrupo, @Param("nuncaConsolidados") boolean nuncaConsolidados,
			@Param("filtrarIds") boolean filtrarIds, @Param("ids") String ids);

	@Query(value = "SELECT m.id " + CANDIDATOS + " AND NOT " + CON_CASO_ABIERTO
			+ " ORDER BY m.apellidos, m.nombres, m.id LIMIT :limite", nativeQuery = true)
	List<Long> idsLote(@Param("iglesiaId") long iglesiaId, @Param("rondaId") long rondaId,
			@Param("condicion") String condicion, @Param("ingresoDesde") LocalDate ingresoDesde,
			@Param("ingresoHasta") LocalDate ingresoHasta, @Param("grupoId") Long grupoId,
			@Param("sinGrupo") boolean sinGrupo, @Param("nuncaConsolidados") boolean nuncaConsolidados,
			@Param("filtrarIds") boolean filtrarIds, @Param("ids") String ids, @Param("limite") int limite);

	/**
	 * Agrega a la ronda las personas del lote en una sola sentencia, por apellidos (los ids siguen ese orden). Quien
	 * ya tiene un caso abierto no entra; {@code ON CONFLICT} cubre a dos coordinadores agregando a la vez.
	 */
	@Modifying
	@Query(value = """
			INSERT INTO caso_consolidacion (iglesia_id, ronda_id, miembro_id, estado, creado_por, creado_en)
			SELECT m.iglesia_id, :rondaId, m.id, 'PENDIENTE', :usuarioId, :ahora
			""" + CANDIDATOS + " AND NOT " + CON_CASO_ABIERTO + """
			 ORDER BY m.apellidos, m.nombres, m.id
			ON CONFLICT DO NOTHING""", nativeQuery = true)
	int agregarLote(@Param("iglesiaId") long iglesiaId, @Param("rondaId") long rondaId,
			@Param("condicion") String condicion, @Param("ingresoDesde") LocalDate ingresoDesde,
			@Param("ingresoHasta") LocalDate ingresoHasta, @Param("grupoId") Long grupoId,
			@Param("sinGrupo") boolean sinGrupo, @Param("nuncaConsolidados") boolean nuncaConsolidados,
			@Param("filtrarIds") boolean filtrarIds, @Param("ids") String ids, @Param("usuarioId") long usuarioId,
			@Param("ahora") Instant ahora);

	@Query("""
			select new com.iglesia.api.consolidacion.model.ResumenRonda(k.rondaId, count(k),
			    sum(case when k.estado = com.iglesia.api.consolidacion.model.EstadoCaso.PENDIENTE then 1 else 0 end),
			    sum(case when k.estado = com.iglesia.api.consolidacion.model.EstadoCaso.EN_CONTACTO then 1 else 0 end),
			    sum(case when k.estado = com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO then 1 else 0 end),
			    sum(case when k.estado = com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO
			             and k.resultadoId is null then 1 else 0 end),
			    sum(case when k.estado <> com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO
			             and k.consolidadorId is null then 1 else 0 end),
			    sum(case when k.estado <> com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO
			             and k.proximoContacto < :hoy then 1 else 0 end))
			from CasoConsolidacion k where k.rondaId in :rondaIds group by k.rondaId""")
	List<ResumenRonda> resumenDe(@Param("rondaIds") Collection<Long> rondaIds, @Param("hoy") LocalDate hoy);

	@Query("""
			select new com.iglesia.api.consolidacion.model.ConteoPorMiembro(k.consolidadorId, count(k))
			from CasoConsolidacion k
			where k.consolidadorId in :miembroIds and k.estado <> com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO
			group by k.consolidadorId""")
	List<ConteoPorMiembro> abiertosPorConsolidador(@Param("miembroIds") Collection<Long> miembroIds);

	Optional<CasoConsolidacion> findFirstByMiembroIdAndEstadoNot(Long miembroId, EstadoCaso estado);

	List<CasoConsolidacion> findByRondaIdAndEstadoNotOrderById(Long rondaId, EstadoCaso estado);

	List<CasoConsolidacion> findByRondaIdOrderById(Long rondaId);

	List<CasoConsolidacion> findByMiembroIdIn(Collection<Long> miembroIds);

	Page<CasoConsolidacion> findByMiembroIdOrderByCreadoEnDescIdDesc(Long miembroId, Pageable paginacion);

	List<CasoConsolidacion> findByMiembroIdOrderByCreadoEnDescIdDesc(Long miembroId);

	boolean existsByConsolidadorIdAndMiembroIdAndEstadoNot(Long consolidadorId, Long miembroId, EstadoCaso estado);

	long countByRondaId(Long rondaId);

	/** Cierra como «No concluido» los casos que siguen abiertos al cerrar la ronda. */
	@Modifying
	@Query("""
			update CasoConsolidacion k
			set k.estado = com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO, k.cerradoEn = :ahora,
			    k.cerradoPor = :usuarioId, k.proximoContacto = null
			where k.rondaId = :rondaId and k.estado <> com.iglesia.api.consolidacion.model.EstadoCaso.CERRADO""")
	int cerrarAbiertosDeRonda(@Param("rondaId") Long rondaId, @Param("usuarioId") Long usuarioId,
			@Param("ahora") Instant ahora);

	@Modifying
	@Query("delete from CasoConsolidacion k where k.rondaId = :rondaId")
	int borrarDeRonda(@Param("rondaId") Long rondaId);

}
