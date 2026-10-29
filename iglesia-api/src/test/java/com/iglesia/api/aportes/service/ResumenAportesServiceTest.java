package com.iglesia.api.aportes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.dto.ResumenAportesResponse;
import com.iglesia.api.aportes.mapper.CategoriaAporteMapper;
import com.iglesia.api.aportes.model.CriterioAportes;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.aportes.model.TotalPorCategoria;
import com.iglesia.api.aportes.model.TotalPorMedio;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.aportes.repository.CategoriaAporteRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
class ResumenAportesServiceTest {

	private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);

	private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

	@Mock
	private AporteRepository aportes;

	@Mock
	private CategoriaAporteRepository categorias;

	@Mock
	private ConsultaAportes consulta;

	@Mock
	private ConfiguracionAportesService configuracion;

	private ResumenAportesService servicio;

	@BeforeEach
	void configurar() {
		servicio = new ResumenAportesService(aportes, categorias, new CategoriaAporteMapper(), consulta,
				configuracion);
		given(configuracion.monedaActual()).willReturn("PEN");
	}

	@Test
	void sumaPorCategoriaEnSuOrdenYComparaConElPeriodoAnteriorDeIgualDuracion() {
		FiltroAportes filtro = new FiltroAportes(DESDE, HASTA, null, null, null, null);
		given(consulta.criterio(filtro)).willReturn(Optional.of(new CriterioAportes(DESDE, HASTA, null, null, null)));
		given(aportes.totalesPorCategoria(DESDE, HASTA, null, null, null)).willReturn(List.of(
				new TotalPorCategoria(2L, new BigDecimal("50.00"), 3L),
				new TotalPorCategoria(1L, new BigDecimal("100.00"), 2L)));
		given(aportes.totalesPorMedio(DESDE, HASTA, null, null, null))
			.willReturn(List.of(new TotalPorMedio(MedioAporte.YAPE, new BigDecimal("150.00"), 5L)));
		given(categorias.findAllById(List.of(2L, 1L))).willReturn(List.of(
				CategoriaAporteServiceTest.categoria(1L, "Diezmo", true),
				CategoriaAporteServiceTest.categoria(2L, "Ofrenda", true)));
		willReturn(new BigDecimal("90.00")).given(aportes)
			.total(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 30), null, null, null);

		ResumenAportesResponse resumen = servicio.resumen(filtro);

		assertThat(resumen.total()).isEqualByComparingTo("150.00");
		assertThat(resumen.cantidad()).isEqualTo(5);
		assertThat(resumen.totalPeriodoAnterior()).isEqualByComparingTo("90.00");
		assertThat(resumen.porCategoria()).extracting(total -> total.categoria().nombre())
			.containsExactly("Diezmo", "Ofrenda");
		assertThat(resumen.moneda()).isEqualTo("PEN");
	}

	@Test
	void sinAmbitoDevuelveTotalesEnCeroSinConsultar() {
		FiltroAportes filtro = new FiltroAportes(DESDE, HASTA, null, null, null, null);
		given(consulta.criterio(filtro)).willReturn(Optional.empty());

		ResumenAportesResponse resumen = servicio.resumen(filtro);

		assertThat(resumen.total()).isZero();
		assertThat(resumen.porCategoria()).isEmpty();
		then(aportes).shouldHaveNoInteractions();
	}

}
