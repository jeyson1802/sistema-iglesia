package com.iglesia.api.miembros.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.component.GeneradorCodigoAsistencia;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.mapper.MiembroMapper;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.miembros.repository.EspecificacionesMiembro;
import com.iglesia.api.miembros.repository.MiembroRepository;

/**
 * Lo que la asistencia necesita de los miembros (buscarlos por código, por id o por nombre) y el código del QR
 * personal, cuya columna pertenece a este contexto. Nunca registra el código en los logs.
 */
@Service
public class MiembrosParaAsistencia {

	private static final Logger LOG = LoggerFactory.getLogger(MiembrosParaAsistencia.class);

	static final String NO_ENCONTRADO = "Miembro no encontrado";

	/** Con 60 bits un choque es casi imposible; el tope solo evita un bucle infinito ante un fallo del azar. */
	static final int INTENTOS_MAX = 5;

	private static final Sort ORDEN_POR_NOMBRE = Sort.by("apellidos", "nombres", "id");

	private final MiembroRepository miembros;

	private final MiembroMapper mapper;

	private final GeneradorCodigoAsistencia generador;

	private final AmbitoMiembros ambito;

	private final UsuarioActual usuarioActual;

	MiembrosParaAsistencia(MiembroRepository miembros, MiembroMapper mapper, GeneradorCodigoAsistencia generador,
			AmbitoMiembros ambito, UsuarioActual usuarioActual) {
		this.miembros = miembros;
		this.mapper = mapper;
		this.generador = generador;
		this.ambito = ambito;
		this.usuarioActual = usuarioActual;
	}

	/** Miembro dueño del código, buscado solo en la iglesia del token. */
	@Transactional(readOnly = true)
	public Optional<MiembroAsistente> porCodigo(CodigoAsistencia codigo) {
		return miembros.findByCodigoAsistencia(codigo.valor()).map(mapper::aAsistente);
	}

	@Transactional(readOnly = true)
	public Optional<MiembroAsistente> porId(Long id) {
		return miembros.findWithNivelByIdIn(List.of(id)).stream().findFirst().map(mapper::aAsistente);
	}

	@Transactional(readOnly = true)
	public Map<Long, MiembroAsistente> porIds(Collection<Long> ids) {
		var validos = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (validos.isEmpty()) {
			return Collections.emptyMap();
		}
		return miembros.findWithNivelByIdIn(validos)
			.stream()
			.map(mapper::aAsistente)
			.collect(Collectors.toMap(MiembroAsistente::id, Function.identity()));
	}

	/** Hasta {@code limite} miembros activos cuyo nombre contiene el texto, por apellidos. */
	@Transactional(readOnly = true)
	public List<MiembroAsistente> buscarActivos(String q, int limite) {
		Specification<Miembro> criterio = Specification.allOf(EspecificacionesMiembro.nombreContiene(q),
				EspecificacionesMiembro.conEstado(true));
		return miembros.findAll(criterio, PageRequest.of(0, limite, ORDEN_POR_NOMBRE)).map(mapper::aAsistente).getContent();
	}

	/** Ids de los miembros (activos o no) cuyo nombre contiene el texto: filtra la lista de asistentes. */
	@Transactional(readOnly = true)
	public List<Long> idsQueCoinciden(String q) {
		return miembros.findAll(EspecificacionesMiembro.nombreContiene(q)).stream().map(Miembro::getId).toList();
	}

	/** Si el miembro existe en la iglesia, sin mirar el ámbito (para la información propia del usuario). */
	@Transactional(readOnly = true)
	public boolean existe(Long miembroId) {
		return miembros.existsById(miembroId);
	}

	/** Si el usuario del token puede ver la ficha del miembro (mismo ámbito que el registro de miembros). */
	@Transactional(readOnly = true)
	public boolean visible(Long miembroId) {
		return ambito.actual().incluye(miembroId) && miembros.existsById(miembroId);
	}

	/** Código del miembro; si aún no tiene (miembros anteriores a la asistencia), se genera ahora. */
	@Transactional
	public CodigoAsistencia codigoDe(Long miembroId) {
		Miembro miembro = visibleOFalla(miembroId);
		if (miembro.getCodigoAsistencia() == null) {
			miembro.asignarCodigoAsistencia(codigoLibre());
			LOG.info("Código de asistencia generado para miembroId={} por usuarioId={}", miembroId, usuario());
		}
		return new CodigoAsistencia(miembro.getCodigoAsistencia());
	}

	/** Código del miembro vinculado al usuario del token (su propio QR); se genera si aún no tiene. */
	@Transactional
	public CodigoAsistencia codigoPropio() {
		Miembro miembro = usuarioActual.miembroId()
			.flatMap(miembros::findById)
			.orElseThrow(() -> new RecursoNoEncontradoException(MiembroService.SIN_MIEMBRO_VINCULADO));
		if (miembro.getCodigoAsistencia() == null) {
			miembro.asignarCodigoAsistencia(codigoLibre());
			LOG.info("Código de asistencia generado para miembroId={} por usuarioId={}", miembro.getId(), usuario());
		}
		return new CodigoAsistencia(miembro.getCodigoAsistencia());
	}

	/** Reemplaza el código: el fotocheck y la imagen anteriores dejan de valer al instante. */
	@Transactional
	public CodigoAsistencia regenerar(Long miembroId) {
		Miembro miembro = visibleOFalla(miembroId);
		CodigoAsistencia nuevo = codigoLibre();
		miembro.asignarCodigoAsistencia(nuevo);
		LOG.info("Código de asistencia regenerado para miembroId={} por usuarioId={}", miembroId, usuario());
		return nuevo;
	}

	/** Código que todavía no usa ningún miembro de la iglesia (lo usa también el alta de miembros). */
	CodigoAsistencia codigoLibre() {
		for (int intento = 0; intento < INTENTOS_MAX; intento++) {
			CodigoAsistencia candidato = generador.generar();
			if (!miembros.existsByCodigoAsistencia(candidato.valor())) {
				return candidato;
			}
		}
		throw new IllegalStateException("No se pudo generar un código de asistencia libre");
	}

	private Miembro visibleOFalla(Long miembroId) {
		return miembros.findById(miembroId)
			.filter(miembro -> ambito.actual().incluye(miembro.getId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
