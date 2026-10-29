package com.iglesia.api.asistencia.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;
import com.iglesia.api.asistencia.dto.AsistenteResponse;
import com.iglesia.api.asistencia.dto.MiembroReunionResponse;
import com.iglesia.api.asistencia.entity.Asistencia;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.Marca;
import com.iglesia.api.asistencia.model.MedioAsistencia;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.repository.AsistenciaRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

/**
 * Marcar y quitar asistencias, y las listas de la toma de asistencia. Marcar a alguien que ya estaba no es un error:
 * responde «ya registrado» para que la fila avance. Los logs llevan solo ids: nunca nombres, códigos ni lo leído.
 */
@Service
public class AsistenciaService {

	private static final Logger LOG = LoggerFactory.getLogger(AsistenciaService.class);

	static final String NO_ES_CODIGO = "Este QR no es un código de asistencia";

	static final String CODIGO_NO_ENCONTRADO = "Código no encontrado en esta iglesia";

	static final String MIEMBRO_NO_ENCONTRADO = "Miembro no encontrado";

	static final String MIEMBRO_INACTIVO = "El miembro está inactivo";

	static final String REUNION_FUTURA = "La reunión aún no ocurre: no se puede tomar asistencia en una fecha futura";

	static final String NO_MARCADO = "El miembro no está marcado en esta reunión";

	static final String SIN_INTEGRANTES = "Solo las reuniones de grupo tienen integrantes";

	private static final Comparator<MiembroAsistente> POR_NOMBRE = Comparator.comparing(MiembroAsistente::nombreCompleto);

	private final AsistenciaRepository asistencias;

	private final ReunionService reuniones;

	private final MiembrosParaAsistencia miembros;

	private final ReferenciasMiembro referencias;

	private final AsistenciaMapper mapper;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	AsistenciaService(AsistenciaRepository asistencias, ReunionService reuniones, MiembrosParaAsistencia miembros,
			ReferenciasMiembro referencias, AsistenciaMapper mapper, IglesiaActual iglesiaActual,
			UsuarioActual usuarioActual, Clock reloj) {
		this.asistencias = asistencias;
		this.reuniones = reuniones;
		this.miembros = miembros;
		this.referencias = referencias;
		this.mapper = mapper;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/** Marca con lo leído del QR o escrito a mano; el código se busca solo en la iglesia del token. */
	@Transactional
	public Marca marcarPorCodigo(Long reunionId, String leido) {
		Reunion reunion = abiertaParaMarcar(reunionId);
		CodigoAsistencia codigo = CodigoAsistencia.leer(leido).orElseThrow(() -> {
			LOG.warn("Lectura que no es un código de asistencia en reunionId={} por usuarioId={}", reunionId, usuario());
			return new ReglaNegocioException(NO_ES_CODIGO, "codigo");
		});
		MiembroAsistente miembro = miembros.porCodigo(codigo).orElseThrow(() -> {
			LOG.warn("Código de asistencia desconocido en reunionId={} por usuarioId={}", reunionId, usuario());
			return new RecursoNoEncontradoException(CODIGO_NO_ENCONTRADO);
		});
		return marcar(reunion, miembro, MedioAsistencia.QR);
	}

	@Transactional
	public Marca marcarMiembro(Long reunionId, Long miembroId) {
		Reunion reunion = abiertaParaMarcar(reunionId);
		MiembroAsistente miembro = miembros.porId(miembroId)
			.orElseThrow(() -> new RecursoNoEncontradoException(MIEMBRO_NO_ENCONTRADO));
		return marcar(reunion, miembro, MedioAsistencia.MANUAL);
	}

	/** El «Deshacer» de la pantalla: borra la marca. */
	@Transactional
	public void quitar(Long reunionId, Long miembroId) {
		reuniones.paraMarcar(reunionId);
		Asistencia asistencia = asistencias.findByReunionIdAndMiembroId(reunionId, miembroId)
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_MARCADO));
		asistencias.delete(asistencia);
		LOG.info("Asistencia quitada reunionId={} miembroId={} por usuarioId={}", reunionId, miembroId, usuario());
	}

	/** Asistentes de la reunión, los últimos marcados primero, filtrados por nombre si se indica. */
	@Transactional(readOnly = true)
	public Page<AsistenteResponse> asistentes(Long reunionId, String q, Pageable paginacion) {
		reuniones.paraVer(reunionId);
		Page<Asistencia> pagina;
		if (q == null || q.isBlank()) {
			pagina = asistencias.findByReunionId(reunionId, paginacion);
		}
		else {
			List<Long> coinciden = miembros.idsQueCoinciden(q);
			pagina = coinciden.isEmpty() ? Page.empty(paginacion)
					: asistencias.findByReunionIdAndMiembroIdIn(reunionId, coinciden, paginacion);
		}
		Map<Long, MiembroAsistente> datos = miembros
			.porIds(pagina.getContent().stream().map(Asistencia::getMiembroId).toList());
		return pagina.map(asistencia -> mapper.aAsistente(asistencia, datos.get(asistencia.getMiembroId())));
	}

	/**
	 * Lista manual de una reunión de grupo: los integrantes activos del grupo, con si están presentes, y al final
	 * quienes vinieron de otro grupo y ya están marcados.
	 */
	@Transactional(readOnly = true)
	public List<MiembroReunionResponse> integrantes(Long reunionId) {
		Reunion reunion = reuniones.paraMarcar(reunionId);
		if (reunion.getTipo() != TipoReunion.GRUPO) {
			throw new ReglaNegocioException(SIN_INTEGRANTES);
		}
		List<IntegranteGrupo> integrantes = referencias.integrantesDe(reunion.getGrupoId());
		Set<Long> presentes = new HashSet<>(asistencias.miembrosDe(reunionId));
		List<MiembroReunionResponse> lista = new ArrayList<>();
		integrantes.forEach(integrante -> lista.add(mapper.aMiembroReunion(new MiembroAsistente(integrante.id(),
				integrante.nombreCompleto(), integrante.nivel(), integrante.grupoId(), true), true,
				presentes.contains(integrante.id()))));
		Set<Long> deOtroGrupo = new HashSet<>(presentes);
		integrantes.forEach(integrante -> deOtroGrupo.remove(integrante.id()));
		miembros.porIds(deOtroGrupo)
			.values()
			.stream()
			.sorted(POR_NOMBRE)
			.forEach(visitante -> lista.add(mapper.aMiembroReunion(visitante, false, true)));
		return lista;
	}

	/** Buscador de la toma manual: miembros activos cuyo nombre contiene el texto, con si ya están presentes. */
	@Transactional(readOnly = true)
	public List<MiembroReunionResponse> buscarMiembros(Long reunionId, String q) {
		Reunion reunion = reuniones.paraMarcar(reunionId);
		if (q == null || q.strip().length() < LimitesAsistencia.BUSQUEDA_MIN) {
			return List.of();
		}
		List<MiembroAsistente> encontrados = miembros.buscarActivos(q, LimitesAsistencia.BUSQUEDA_MAX);
		if (encontrados.isEmpty()) {
			return List.of();
		}
		Set<Long> presentes = new HashSet<>(asistencias.presentesEntre(reunionId,
				encontrados.stream().map(MiembroAsistente::id).collect(Collectors.toSet())));
		return encontrados.stream()
			.map(miembro -> mapper.aMiembroReunion(miembro,
					reunion.getGrupoId() != null && reunion.getGrupoId().equals(miembro.grupoId()),
					presentes.contains(miembro.id())))
			.toList();
	}

	private Marca marcar(Reunion reunion, MiembroAsistente miembro, MedioAsistencia medio) {
		if (!miembro.activo()) {
			throw new ConflictoException(MIEMBRO_INACTIVO);
		}
		boolean nueva = asistencias.marcar(iglesiaActual.requerida(), reunion.getId(), miembro.id(), medio.name(),
				usuario(), Instant.now(reloj)) == 1;
		Asistencia asistencia = asistencias.findByReunionIdAndMiembroId(reunion.getId(), miembro.id()).orElseThrow();
		if (nueva) {
			LOG.info("Asistencia marcada reunionId={} miembroId={} medio={} por usuarioId={}", reunion.getId(),
					miembro.id(), medio, usuario());
		}
		long total = asistencias.countByReunionId(reunion.getId());
		return new Marca(mapper.aMarca(miembro, asistencia, !nueva, total), nueva);
	}

	private Reunion abiertaParaMarcar(Long reunionId) {
		Reunion reunion = reuniones.paraMarcar(reunionId);
		if (reunion.getFecha().isAfter(reuniones.hoyEnLaIglesia())) {
			throw new ReglaNegocioException(REUNION_FUTURA);
		}
		return reunion;
	}

	private Long usuario() {
		return usuarioActual.id().orElseThrow(() -> new IllegalStateException("Petición sin usuario autenticado"));
	}

}
