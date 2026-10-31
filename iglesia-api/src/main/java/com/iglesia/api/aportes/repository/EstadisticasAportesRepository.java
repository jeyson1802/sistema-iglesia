package com.iglesia.api.aportes.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.iglesia.api.aportes.entity.Aporte;

/** Total de los aportes vigentes (sin anulados) de toda la iglesia por día, semana o mes. */
public interface EstadisticasAportesRepository extends Repository<Aporte, Long> {

	@Query(value = """
			SELECT CAST(date_trunc(:unidad, CAST(fecha AS timestamp)) AS date) AS inicio, sum(monto) AS total,
			       count(*) AS cantidad
			FROM aporte
			WHERE iglesia_id = :iglesiaId AND NOT anulado AND fecha BETWEEN :desde AND :hasta
			GROUP BY 1 ORDER BY 1""", nativeQuery = true)
	List<FilaSerie> serie(@Param("iglesiaId") long iglesiaId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta, @Param("unidad") String unidad);

	interface FilaSerie {

		LocalDate getInicio();

		BigDecimal getTotal();

		long getCantidad();

	}

}
