package com.iglesia.api.estadisticas.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.estadisticas.util.Series;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.api.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.api.common.periodo.dto.PeriodoResponse;
import com.iglesia.api.common.periodo.model.RangoPeriodo;
import com.iglesia.api.estadisticas.constant.LimitesEstadisticasSistema;
import com.iglesia.api.estadisticas.dto.EstadisticasSistemaResponse;
import com.iglesia.api.estadisticas.dto.IglesiaMiembrosResponse;
import com.iglesia.api.estadisticas.dto.PuntoSistemaResponse;
import com.iglesia.api.estadisticas.repository.EstadisticasSistemaRepository.FilaConteo;
import com.iglesia.api.estadisticas.repository.EstadisticasSistemaRepository.FilaResumen;
import com.iglesia.api.estadisticas.repository.EstadisticasSistemaRepository.FilaSerie;
import com.iglesia.api.estadisticas.repository.EstadisticasSistemaRepository;
import com.iglesia.api.miembros.model.CondicionMiembro;

/**
 * Cifras entre iglesias del administrador del sistema. Solo en la vista global: dentro de una iglesia opera como su
 * administrador y estas cifras no aplican (403).
 */
@Service
public class EstadisticasSistemaService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasSistemaService.class);

	static final String SOLO_VISTA_GLOBAL = "Las estadísticas del sistema solo se ven en la vista global";

	private static final String SIN_DATO = "SIN_DATO";

	private final EstadisticasSistemaRepository consultas;

	private final CalculadoraPeriodo calculadora;

	private final IglesiaActual iglesiaActual;

	EstadisticasSistemaService(EstadisticasSistemaRepository consultas, CalculadoraPeriodo calculadora,
			IglesiaActual iglesiaActual) {
		this.consultas = consultas;
		this.calculadora = calculadora;
		this.iglesiaActual = iglesiaActual;
	}

	@Transactional(readOnly = true)
	public EstadisticasSistemaResponse calcular(ConsultaPeriodo periodo) {
		if (iglesiaActual.id().isPresent()) {
			throw new AccessDeniedException(SOLO_VISTA_GLOBAL);
		}
		RangoPeriodo rango = calculadora.rango(periodo);
		LOG.debug("Estadísticas del sistema periodo={}", periodo);
		FilaResumen resumen = consultas.resumen(rango.desde(), rango.hasta(), rango.anteriorDesde(),
				rango.anteriorHasta());
		List<PuntoSistemaResponse> serie = Series.rellenar(rango,
				consultas.serie(rango.desde(), rango.hasta(), rango.granularidad().unidadSql()), FilaSerie::getInicio,
				(inicio, fila) -> fila == null ? new PuntoSistemaResponse(inicio, 0, 0, 0, 0, 0, 0)
						: new PuntoSistemaResponse(inicio, fila.getIglesiasNuevas(), fila.getAltas(), fila.getBajas(),
								fila.getMiembrosRegistrados(), fila.getAsistenciasMarcadas(),
								fila.getContactosConsolidacion()));
		return new EstadisticasSistemaResponse(PeriodoResponse.de(rango), resumen.getIglesias(),
				resumen.getIglesiasNuevas(), resumen.getIglesiasNuevasAnterior(), resumen.getIglesiasConActividad(),
				resumen.getIglesiasConActividadAnterior(), resumen.getMiembros(), resumen.getAltas(),
				resumen.getAltasAnterior(), resumen.getBajas(), resumen.getBajasAnterior(), serie,
				condiciones(consultas.condiciones()), tamanos(consultas.tamanos()),
				consultas.mayores(LimitesEstadisticasSistema.IGLESIAS_MAYORES)
					.stream()
					.map(fila -> new IglesiaMiembrosResponse(fila.getIglesiaId(), fila.getNombre(), fila.getMiembros()))
					.toList());
	}

	private static List<ConteoResponse> condiciones(List<FilaConteo> filas) {
		Map<String, Long> valores = porClave(filas);
		var conocidas = Arrays.stream(CondicionMiembro.values())
			.map(condicion -> new ConteoResponse(condicion.name(), condicion.etiqueta(),
					valores.getOrDefault(condicion.name(), 0L)));
		long sinDato = valores.getOrDefault(SIN_DATO, 0L);
		return sinDato == 0 ? conocidas.toList()
				: Stream.concat(conocidas, Stream.of(
						new ConteoResponse(SIN_DATO, "Sin dato", sinDato))).toList();
	}

	private static List<ConteoResponse> tamanos(List<FilaConteo> filas) {
		Map<String, Long> valores = porClave(filas);
		return Arrays.stream(TamanoIglesia.values())
			.map(tamano -> new ConteoResponse(tamano.name(), tamano.etiqueta(), valores.getOrDefault(tamano.name(), 0L)))
			.toList();
	}

	private static Map<String, Long> porClave(List<FilaConteo> filas) {
		return filas.stream().collect(Collectors.toMap(FilaConteo::getClave, FilaConteo::getValor));
	}

	/** Tramos de {@code estadisticas_sistema_tamanos}, de menor a mayor. */
	enum TamanoIglesia {

		SIN_MIEMBROS("Sin miembros"), HASTA_50("1 a 50"), HASTA_200("51 a 200"), HASTA_500("201 a 500"),
		HASTA_1000("501 a 1000"), MAS_DE_1000("Más de 1000");

		private final String etiqueta;

		TamanoIglesia(String etiqueta) {
			this.etiqueta = etiqueta;
		}

		String etiqueta() {
			return etiqueta;
		}

	}

}
