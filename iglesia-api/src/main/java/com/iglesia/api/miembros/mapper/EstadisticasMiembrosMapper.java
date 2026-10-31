package com.iglesia.api.miembros.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.estadisticas.dto.ConteoResponse;
import com.iglesia.api.common.estadisticas.util.Series;
import com.iglesia.api.common.periodo.model.RangoPeriodo;
import com.iglesia.api.miembros.dto.PuntoAltasBajasResponse;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.miembros.repository.EstadisticasMiembrosRepository.FilaConteo;
import com.iglesia.api.miembros.repository.EstadisticasMiembrosRepository.FilaSerie;

/** Etiquetas en español y orden fijo de las distribuciones de miembros. */
@Component
public class EstadisticasMiembrosMapper {

	static final String SIN_DATO = "SIN_DATO";

	static final String SIN_NIVEL = "SIN_NIVEL";

	private static final String ETIQUETA_SIN_DATO = "Sin dato";

	/** Códigos en el orden en que se muestran, con su etiqueta. */
	private static final List<Map.Entry<String, String>> CONDICIONES = List.of(
			Map.entry(CondicionMiembro.NUEVO.name(), CondicionMiembro.NUEVO.etiqueta()),
			Map.entry(CondicionMiembro.RECUPERADO.name(), CondicionMiembro.RECUPERADO.etiqueta()),
			Map.entry(CondicionMiembro.CONSOLIDADO.name(), CondicionMiembro.CONSOLIDADO.etiqueta()),
			Map.entry(SIN_DATO, ETIQUETA_SIN_DATO));

	private static final List<Map.Entry<String, String>> GENEROS = List.of(Map.entry("FEMENINO", "Mujeres"),
			Map.entry("MASCULINO", "Hombres"), Map.entry(SIN_DATO, ETIQUETA_SIN_DATO));

	private static final List<Map.Entry<String, String>> EDADES = List.of(Map.entry("NINOS", "0 a 12"),
			Map.entry("ADOLESCENTES", "13 a 17"), Map.entry("JOVENES", "18 a 29"),
			Map.entry("ADULTOS_JOVENES", "30 a 44"), Map.entry("ADULTOS", "45 a 59"), Map.entry("MAYORES", "60 o más"),
			Map.entry(SIN_DATO, ETIQUETA_SIN_DATO));

	public List<PuntoAltasBajasResponse> serie(RangoPeriodo rango, List<FilaSerie> filas) {
		return Series.rellenar(rango, filas, FilaSerie::getInicio, (inicio, fila) -> fila == null
				? new PuntoAltasBajasResponse(inicio, 0, 0) : new PuntoAltasBajasResponse(inicio, fila.getAltas(), fila.getBajas()));
	}

	public List<ConteoResponse> condiciones(List<FilaConteo> filas) {
		return conCatalogo(CONDICIONES, filas);
	}

	public List<ConteoResponse> generos(List<FilaConteo> filas) {
		return conCatalogo(GENEROS, filas);
	}

	public List<ConteoResponse> edades(List<FilaConteo> filas) {
		return conCatalogo(EDADES, filas);
	}

	/** Ya vienen en el orden de los niveles; los que no tienen nivel van al final como «Sin nivel». */
	public List<ConteoResponse> niveles(List<FilaConteo> filas) {
		return filas.stream()
			.map(fila -> new ConteoResponse(fila.getClave(),
					SIN_NIVEL.equals(fila.getClave()) ? "Sin nivel" : fila.getEtiqueta(), fila.getValor()))
			.sorted(Comparator.comparing((ConteoResponse conteo) -> SIN_NIVEL.equals(conteo.clave())))
			.toList();
	}

	/** Todas las categorías del catálogo en su orden, con cero si no hay filas; «Sin dato» solo si tiene. */
	private static List<ConteoResponse> conCatalogo(List<Map.Entry<String, String>> catalogo, List<FilaConteo> filas) {
		Map<String, Long> valores = filas.stream().collect(Collectors.toMap(FilaConteo::getClave, FilaConteo::getValor));
		return catalogo.stream()
			.map(entrada -> new ConteoResponse(entrada.getKey(), entrada.getValue(),
					valores.getOrDefault(entrada.getKey(), 0L)))
			.filter(conteo -> !SIN_DATO.equals(conteo.clave()) || conteo.valor() > 0)
			.toList();
	}

}
