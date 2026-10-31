package com.iglesia.api.aportes.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.periodo.dto.PeriodoResponse;

@Schema(description = "Aportes vigentes de toda la iglesia en el periodo: totales, desglose y serie.")
public record EstadisticasAportesResponse(
		@Schema(description = "Fechas usadas y las del periodo anterior.", example = "{\"periodo\": \"MES\", "
				+ "\"desde\": \"2026-10-01\", \"hasta\": \"2026-10-30\", \"anteriorDesde\": \"2026-09-01\", "
				+ "\"anteriorHasta\": \"2026-09-30\", \"granularidad\": \"SEMANA\"}") PeriodoResponse periodo,
		@Schema(description = "Total, cantidad, total del periodo anterior y desglose por categoría y por medio.",
				example = "{\"moneda\": \"PEN\", \"total\": 3420.50, \"cantidad\": 41, \"totalPeriodoAnterior\": 2980.00, "
						+ "\"porCategoria\": [], \"porMedio\": []}") ResumenAportesResponse resumen,
		@Schema(description = "Total por día, semana o mes, sin huecos.",
				example = "[{\"inicio\": \"2026-10-05\", \"total\": 1250.00, \"cantidad\": 18}]") List<PuntoAportesResponse> serie) {

	public EstadisticasAportesResponse {
		serie = List.copyOf(serie);
	}

}
