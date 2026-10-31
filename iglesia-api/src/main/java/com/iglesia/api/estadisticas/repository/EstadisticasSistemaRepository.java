package com.iglesia.api.estadisticas.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.iglesia.entity.Iglesia;

/**
 * Llama a las funciones {@code estadisticas_sistema_*} (migración V12): las únicas consultas entre iglesias. Son
 * {@code SECURITY DEFINER} y devuelven solo agregados, así que funcionan sin iglesia fijada y sin desactivar la RLS.
 */
public interface EstadisticasSistemaRepository extends Repository<Iglesia, Long> {

	@Query(value = """
			SELECT iglesias, iglesias_nuevas AS iglesiasNuevas, iglesias_nuevas_anterior AS iglesiasNuevasAnterior,
			       iglesias_con_actividad AS iglesiasConActividad,
			       iglesias_con_actividad_anterior AS iglesiasConActividadAnterior, miembros,
			       miembros_altas AS altas, miembros_altas_anterior AS altasAnterior, miembros_bajas AS bajas,
			       miembros_bajas_anterior AS bajasAnterior
			FROM estadisticas_sistema_resumen(:desde, :hasta, :anteriorDesde, :anteriorHasta)""", nativeQuery = true)
	FilaResumen resumen(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("anteriorDesde") LocalDate anteriorDesde, @Param("anteriorHasta") LocalDate anteriorHasta);

	@Query(value = """
			SELECT inicio, iglesias_nuevas AS iglesiasNuevas, miembros_altas AS altas, miembros_bajas AS bajas,
			       miembros_registrados AS miembrosRegistrados, asistencias_marcadas AS asistenciasMarcadas,
			       contactos_consolidacion AS contactosConsolidacion
			FROM estadisticas_sistema_serie(:desde, :hasta, :unidad)""", nativeQuery = true)
	List<FilaSerie> serie(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
			@Param("unidad") String unidad);

	@Query(value = "SELECT COALESCE(condicion, 'SIN_DATO') AS clave, total AS valor FROM estadisticas_sistema_condiciones()",
			nativeQuery = true)
	List<FilaConteo> condiciones();

	@Query(value = "SELECT rango AS clave, total AS valor FROM estadisticas_sistema_tamanos()", nativeQuery = true)
	List<FilaConteo> tamanos();

	@Query(value = """
			SELECT iglesia_id AS iglesiaId, nombre, miembros
			FROM estadisticas_sistema_mayores(:limite)""", nativeQuery = true)
	List<FilaIglesia> mayores(@Param("limite") int limite);

	interface FilaResumen {

		long getIglesias();

		long getIglesiasNuevas();

		long getIglesiasNuevasAnterior();

		long getIglesiasConActividad();

		long getIglesiasConActividadAnterior();

		long getMiembros();

		long getAltas();

		long getAltasAnterior();

		long getBajas();

		long getBajasAnterior();

	}

	interface FilaSerie {

		LocalDate getInicio();

		long getIglesiasNuevas();

		long getAltas();

		long getBajas();

		long getMiembrosRegistrados();

		long getAsistenciasMarcadas();

		long getContactosConsolidacion();

	}

	interface FilaConteo {

		String getClave();

		long getValor();

	}

	interface FilaIglesia {

		Long getIglesiaId();

		String getNombre();

		long getMiembros();

	}

}
