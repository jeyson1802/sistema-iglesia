package com.iglesia.api.consolidacion.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.consolidacion.dto.ResultadoConsolidacionResponse;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.repository.ResultadoConsolidacionRepository;

/**
 * Catálogo de resultados de la iteración de cada iglesia, como los niveles: se renombran, se ordenan y se desactivan
 * (nunca se borran, para conservar el historial).
 */
@Service
public class ResultadoConsolidacionService {

	private static final Logger LOG = LoggerFactory.getLogger(ResultadoConsolidacionService.class);

	static final String NO_ENCONTRADO = "Resultado no encontrado";

	static final String NOMBRE_REPETIDO = "Ya existe un resultado con ese nombre";

	static final String ORDEN_INCOMPLETO = "El orden debe incluir todos los resultados de la iglesia, una sola vez";

	static final String RESULTADO_INACTIVO = "Elige un resultado activo del catálogo";

	private static final String CAMPO_NOMBRE = "nombre";

	private static final String CAMPO_RESULTADO = "resultadoId";

	private final ResultadoConsolidacionRepository resultados;

	private final ConsolidacionMapper mapper;

	private final UsuarioActual usuarioActual;

	ResultadoConsolidacionService(ResultadoConsolidacionRepository resultados, ConsolidacionMapper mapper,
			UsuarioActual usuarioActual) {
		this.resultados = resultados;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<ResultadoConsolidacionResponse> listar() {
		return resultados.findAllByOrderByOrdenAscIdAsc().stream().map(mapper::aResultado).toList();
	}

	@Transactional
	public ResultadoConsolidacionResponse crear(String nombre) {
		String normalizado = nombreLibre(nombre, null);
		ResultadoConsolidacion creado = resultados
			.save(ResultadoConsolidacion.nuevo(normalizado, resultados.ordenMaximo() + 1));
		LOG.info("Resultado de consolidación creado resultadoId={} por usuarioId={}", creado.getId(), usuario());
		return mapper.aResultado(creado);
	}

	@Transactional
	public ResultadoConsolidacionResponse renombrar(Long id, String nombre) {
		ResultadoConsolidacion resultado = buscar(id);
		resultado.renombrar(nombreLibre(nombre, id));
		LOG.info("Resultado de consolidación renombrado resultadoId={} por usuarioId={}", id, usuario());
		return mapper.aResultado(resultado);
	}

	@Transactional
	public ResultadoConsolidacionResponse cambiarEstado(Long id, boolean activo) {
		ResultadoConsolidacion resultado = buscar(id);
		resultado.cambiarEstado(activo);
		LOG.info("Resultado de consolidación resultadoId={} activo={} por usuarioId={}", id, activo, usuario());
		return mapper.aResultado(resultado);
	}

	@Transactional
	public List<ResultadoConsolidacionResponse> ordenar(List<Long> ids) {
		List<ResultadoConsolidacion> todos = resultados.findAllByOrderByOrdenAscIdAsc();
		boolean completo = ids.size() == todos.size() && new HashSet<>(ids).size() == ids.size()
				&& todos.stream().allMatch(resultado -> ids.contains(resultado.getId()));
		if (!completo) {
			throw new ReglaNegocioException(ORDEN_INCOMPLETO, "ids");
		}
		Map<Long, ResultadoConsolidacion> porId = todos.stream()
			.collect(Collectors.toMap(ResultadoConsolidacion::getId, Function.identity()));
		for (int posicion = 0; posicion < ids.size(); posicion++) {
			porId.get(ids.get(posicion)).ordenar(posicion + 1);
		}
		LOG.info("Resultados de consolidación reordenados por usuarioId={}", usuario());
		return listar();
	}

	/** Resultado elegido al cerrar un caso: debe existir en la iglesia y estar activo; si no, 400. */
	ResultadoConsolidacion activo(Long id) {
		return resultados.findById(id)
			.filter(ResultadoConsolidacion::isActivo)
			.orElseThrow(() -> new ReglaNegocioException(RESULTADO_INACTIVO, CAMPO_RESULTADO));
	}

	Map<Long, ResultadoConsolidacion> todosPorId() {
		return resultados.findAll()
			.stream()
			.collect(Collectors.toMap(ResultadoConsolidacion::getId, Function.identity()));
	}

	private ResultadoConsolidacion buscar(Long id) {
		return resultados.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	private String nombreLibre(String nombre, Long idExcluido) {
		String normalizado = Textos.normalizar(nombre);
		boolean ocupado = resultados.findAll()
			.stream()
			.anyMatch(otro -> !otro.getId().equals(idExcluido) && Textos.mismoTexto(otro.getNombre(), normalizado));
		if (ocupado) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE);
		}
		return normalizado;
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
