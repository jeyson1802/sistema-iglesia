package com.iglesia.api.aportes.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.aportes.dto.CategoriaAporteResponse;
import com.iglesia.api.aportes.dto.GuardarCategoriaAporteRequest;
import com.iglesia.api.aportes.dto.OrdenarCategoriasAporteRequest;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.aportes.mapper.CategoriaAporteMapper;
import com.iglesia.api.aportes.repository.CategoriaAporteRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;

/**
 * Categorías de aporte de la iglesia del token. El nombre es único sin distinguir mayúsculas (el índice
 * {@code ux_categoria_aporte_nombre} lo respalda); las nuevas van al final del orden. Nunca se borran: se desactivan, y
 * sus aportes siguen contando en los totales.
 */
@Service
public class CategoriaAporteService {

	private static final Logger LOG = LoggerFactory.getLogger(CategoriaAporteService.class);

	static final String NO_ENCONTRADA = "Categoría de aporte no encontrada";

	static final String NOMBRE_REPETIDO = "Ya existe una categoría con ese nombre";

	static final String ORDEN_INCOMPLETO = "El orden debe incluir todas las categorías de la iglesia, una sola vez "
			+ "cada una";

	static final String INEXISTENTE = "La categoría no existe";

	static final String INACTIVA = "La categoría está desactivada y no se puede usar en aportes nuevos";

	private static final String CAMPO_NOMBRE = "nombre";

	private static final String CAMPO_CATEGORIA = "categoriaId";

	private final CategoriaAporteRepository categorias;

	private final CategoriaAporteMapper mapper;

	private final UsuarioActual usuarioActual;

	CategoriaAporteService(CategoriaAporteRepository categorias, CategoriaAporteMapper mapper,
			UsuarioActual usuarioActual) {
		this.categorias = categorias;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<CategoriaAporteResponse> listar() {
		return categorias.findAllByOrderByOrdenAscIdAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional
	public CategoriaAporteResponse crear(GuardarCategoriaAporteRequest solicitud) {
		String nombre = Textos.normalizar(solicitud.nombre());
		if (nombreOcupado(nombre, null)) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE);
		}
		CategoriaAporte categoria = guardar(
				CategoriaAporte.nueva(nombre, solicitud.exigeAportante(), categorias.ultimoOrden() + 1));
		LOG.info("Categoría de aporte creada id={} por usuarioId={}", categoria.getId(),
				usuarioActual.id().orElse(null));
		return mapper.aRespuesta(categoria);
	}

	@Transactional
	public CategoriaAporteResponse actualizar(Long id, GuardarCategoriaAporteRequest solicitud) {
		CategoriaAporte categoria = buscar(id);
		String nombre = Textos.normalizar(solicitud.nombre());
		if (nombreOcupado(nombre, id)) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE);
		}
		categoria.actualizar(nombre, solicitud.exigeAportante());
		guardar(categoria);
		LOG.info("Categoría de aporte actualizada id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return mapper.aRespuesta(categoria);
	}

	/** Reordena todas las categorías: la lista debe traer cada una exactamente una vez. */
	@Transactional
	public List<CategoriaAporteResponse> ordenar(OrdenarCategoriasAporteRequest solicitud) {
		List<CategoriaAporte> todas = categorias.findAllByOrderByOrdenAscIdAsc();
		Set<Long> existentes = todas.stream().map(CategoriaAporte::getId).collect(Collectors.toSet());
		if (solicitud.ids().size() != todas.size() || !existentes.equals(new HashSet<>(solicitud.ids()))) {
			throw new ReglaNegocioException(ORDEN_INCOMPLETO, "ids");
		}
		for (CategoriaAporte categoria : todas) {
			categoria.ordenar(solicitud.ids().indexOf(categoria.getId()) + 1);
		}
		LOG.info("Categorías de aporte reordenadas por usuarioId={}", usuarioActual.id().orElse(null));
		return categorias.findAllByOrderByOrdenAscIdAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional
	public CategoriaAporteResponse cambiarEstado(Long id, boolean activo) {
		CategoriaAporte categoria = buscar(id);
		categoria.cambiarEstado(activo);
		LOG.info("Categoría de aporte {} id={} por usuarioId={}", activo ? "reactivada" : "desactivada", id,
				usuarioActual.id().orElse(null));
		return mapper.aRespuesta(categoria);
	}

	/**
	 * Categoría que se asigna a un aporte. Si no cambia respecto de la actual se acepta aunque esté inactiva (los
	 * aportes ya registrados la conservan); si cambia, debe existir en la iglesia y estar activa (409).
	 */
	CategoriaAporte asignable(Long nuevaId, CategoriaAporte actual) {
		if (actual != null && Objects.equals(actual.getId(), nuevaId)) {
			return actual;
		}
		CategoriaAporte categoria = categorias.findById(nuevaId)
			.orElseThrow(() -> new ConflictoException(INEXISTENTE, CAMPO_CATEGORIA));
		if (!categoria.isActivo()) {
			throw new ConflictoException(INACTIVA, CAMPO_CATEGORIA);
		}
		return categoria;
	}

	/** El catálogo es pequeño: se compara en Java para no depender de la configuración regional de la base. */
	private boolean nombreOcupado(String nombre, Long idExcluido) {
		return categorias.findAll()
			.stream()
			.anyMatch(categoria -> !categoria.getId().equals(idExcluido)
					&& Textos.mismoTexto(categoria.getNombre(), nombre));
	}

	private CategoriaAporte buscar(Long id) {
		return categorias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
	}

	/** Escribe ya para que un nombre repetido por un alta simultánea responda 409 y no 500. */
	private CategoriaAporte guardar(CategoriaAporte categoria) {
		try {
			return categorias.saveAndFlush(categoria);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE);
		}
	}

}
