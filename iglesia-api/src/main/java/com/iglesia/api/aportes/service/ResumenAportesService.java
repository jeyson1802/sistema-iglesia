package com.iglesia.api.aportes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.dto.ResumenAportesResponse;
import com.iglesia.api.aportes.dto.TotalCategoriaResponse;
import com.iglesia.api.aportes.dto.TotalMedioResponse;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.aportes.mapper.CategoriaAporteMapper;
import com.iglesia.api.aportes.model.CriterioAportes;
import com.iglesia.api.aportes.model.TotalPorCategoria;
import com.iglesia.api.aportes.model.TotalPorMedio;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.aportes.repository.CategoriaAporteRepository;

/**
 * Totales de los aportes vigentes del rango, agrupados en la base por categoría y por medio, y el total del período
 * anterior de la misma duración para comparar. Respeta el ámbito del token igual que el listado.
 */
@Service
public class ResumenAportesService {

	private final AporteRepository aportes;

	private final CategoriaAporteRepository categorias;

	private final CategoriaAporteMapper mapperCategorias;

	private final ConsultaAportes consulta;

	private final ConfiguracionAportesService configuracion;

	ResumenAportesService(AporteRepository aportes, CategoriaAporteRepository categorias,
			CategoriaAporteMapper mapperCategorias, ConsultaAportes consulta,
			ConfiguracionAportesService configuracion) {
		this.aportes = aportes;
		this.categorias = categorias;
		this.mapperCategorias = mapperCategorias;
		this.consulta = consulta;
		this.configuracion = configuracion;
	}

	@Transactional(readOnly = true)
	public ResumenAportesResponse resumen(FiltroAportes filtro) {
		String moneda = configuracion.monedaActual();
		return consulta.criterio(filtro)
			.map(criterio -> calcular(criterio, moneda))
			.orElseGet(() -> new ResumenAportesResponse(moneda, BigDecimal.ZERO, 0, BigDecimal.ZERO, List.of(),
					List.of()));
	}

	private ResumenAportesResponse calcular(CriterioAportes criterio, String moneda) {
		List<TotalPorCategoria> porCategoria = aportes.totalesPorCategoria(criterio.desde(), criterio.hasta(),
				criterio.categoriaId(), criterio.medio(), criterio.miembroId());
		List<TotalPorMedio> porMedio = aportes.totalesPorMedio(criterio.desde(), criterio.hasta(),
				criterio.categoriaId(), criterio.medio(), criterio.miembroId());
		BigDecimal total = porCategoria.stream().map(TotalPorCategoria::total).reduce(BigDecimal.ZERO, BigDecimal::add);
		long cantidad = porCategoria.stream().mapToLong(TotalPorCategoria::cantidad).sum();
		return new ResumenAportesResponse(moneda, total, cantidad, totalAnterior(criterio),
				aRespuestaPorCategoria(porCategoria), aRespuestaPorMedio(porMedio));
	}

	/** El período anterior termina el día antes de {@code desde} y dura lo mismo que el consultado. */
	private BigDecimal totalAnterior(CriterioAportes criterio) {
		long dias = ChronoUnit.DAYS.between(criterio.desde(), criterio.hasta()) + 1;
		LocalDate hasta = criterio.desde().minusDays(1);
		CriterioAportes anterior = criterio.entre(hasta.minusDays(dias - 1), hasta);
		return aportes.total(anterior.desde(), anterior.hasta(), anterior.categoriaId(), anterior.medio(),
				anterior.miembroId());
	}

	/** En el orden de las categorías, con su nombre resuelto en una sola consulta. */
	private List<TotalCategoriaResponse> aRespuestaPorCategoria(List<TotalPorCategoria> totales) {
		Map<Long, CategoriaAporte> porId = categorias
			.findAllById(totales.stream().map(TotalPorCategoria::categoriaId).toList())
			.stream()
			.collect(Collectors.toMap(CategoriaAporte::getId, Function.identity()));
		return totales.stream()
			.sorted(Comparator.comparing((TotalPorCategoria total) -> porId.get(total.categoriaId()).getOrden())
				.thenComparing(TotalPorCategoria::categoriaId))
			.map(total -> new TotalCategoriaResponse(mapperCategorias.aReferencia(porId.get(total.categoriaId())),
					total.total(), total.cantidad()))
			.toList();
	}

	/** En el orden fijo de los medios. */
	private static List<TotalMedioResponse> aRespuestaPorMedio(List<TotalPorMedio> totales) {
		return totales.stream()
			.sorted(Comparator.comparing(TotalPorMedio::medio))
			.map(total -> new TotalMedioResponse(total.medio(), total.total(), total.cantidad()))
			.toList();
	}

}
