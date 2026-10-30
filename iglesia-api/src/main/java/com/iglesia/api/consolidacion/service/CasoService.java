package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.service.AsistenciaParaConsolidacion;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.consolidacion.dto.AbrirSeguimientoRequest;
import com.iglesia.api.consolidacion.dto.AntesDeContactarResponse;
import com.iglesia.api.consolidacion.dto.CasoDetalleResponse;
import com.iglesia.api.consolidacion.dto.CasoResumenResponse;
import com.iglesia.api.consolidacion.dto.CerrarCasoRequest;
import com.iglesia.api.consolidacion.dto.ContactoPersonaResponse;
import com.iglesia.api.consolidacion.dto.FiltroCasosRonda;
import com.iglesia.api.consolidacion.dto.FiltroMisCasos;
import com.iglesia.api.consolidacion.dto.ResumenMisCasosResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.SeccionMisCasos;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.EspecificacionesConsolidacion;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.service.AgendaGrupos;
import com.iglesia.api.miembros.model.ContactoPersona;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Casos de consolidación: listas (de una ronda y «Mis asignados»), detalle para contactar, seguimiento individual,
 * cierre y reapertura. El ámbito lo decide {@link AmbitoConsolidacion}: fuera de él, 404.
 */
@Service
public class CasoService {

	private static final Logger LOG = LoggerFactory.getLogger(CasoService.class);

	static final String NO_ENCONTRADO = "Caso no encontrado";

	static final String YA_CERRADO = "El caso ya está cerrado";

	static final String NO_CERRADO = "El caso no está cerrado";

	static final String RONDA_NO_EN_CURSO = "Solo se trabaja en casos de rondas en curso o en seguimientos individuales";

	static final String CASO_ABIERTO_EN_RONDA = "Ya tiene un seguimiento abierto en la ronda «%s»";

	static final String CASO_ABIERTO_INDIVIDUAL = "Ya tiene un seguimiento individual abierto";

	static final String SIN_CONSOLIDADOR = "Indica quién lo consolidará";

	static final String SOLO_COORDINADOR_ELIGE = "Solo el coordinador puede asignar el seguimiento a otra persona";

	static final String NO_PUEDE_INICIAR = "Solo puedes iniciar seguimientos a personas de tu red";

	static final String NO_PUEDE_ACTUAR = "Solo el consolidador asignado o el coordinador pueden trabajar este caso";

	private static final String CAMPO_CONSOLIDADOR = "consolidadorId";

	private static final Sort POR_LOTE = Sort.by("id");

	private final CasoConsolidacionRepository casos;

	private final RondaService rondas;

	private final ResultadoConsolidacionService resultados;

	private final EquipoConsolidacionService equipo;

	private final ResumenesDeCasos resumenes;

	private final AmbitoConsolidacion ambito;

	private final MiembrosParaConsolidacion miembros;

	private final AsistenciaParaConsolidacion asistencia;

	private final AgendaGrupos grupos;

	private final ConsolidacionMapper mapper;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	CasoService(CasoConsolidacionRepository casos, RondaService rondas, ResultadoConsolidacionService resultados,
			EquipoConsolidacionService equipo, ResumenesDeCasos resumenes, AmbitoConsolidacion ambito,
			MiembrosParaConsolidacion miembros, AsistenciaParaConsolidacion asistencia, AgendaGrupos grupos,
			ConsolidacionMapper mapper, UsuarioActual usuarioActual, Clock reloj) {
		this.casos = casos;
		this.rondas = rondas;
		this.resultados = resultados;
		this.equipo = equipo;
		this.resumenes = resumenes;
		this.ambito = ambito;
		this.miembros = miembros;
		this.asistencia = asistencia;
		this.grupos = grupos;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/** Casos de una ronda en el orden del lote (por apellidos). */
	@Transactional(readOnly = true)
	public Page<CasoResumenResponse> deRonda(Long rondaId, FiltroCasosRonda filtro, Pageable paginacion) {
		rondas.buscar(rondaId);
		Specification<CasoConsolidacion> criterio = Specification.allOf(EspecificacionesConsolidacion.deRonda(rondaId),
				EspecificacionesConsolidacion.conEstado(filtro.estado()),
				EspecificacionesConsolidacion.deConsolidador(filtro.consolidadorId()),
				EspecificacionesConsolidacion.sinAsignar(filtro.soloSinAsignar()),
				EspecificacionesConsolidacion.dePersonas(personasQueCoinciden(filtro.q())));
		return pagina(criterio, paginacion, POR_LOTE);
	}

	/**
	 * «Mis asignados»: casos abiertos del miembro del token que ya puede trabajar, por sección. Para hoy, del más
	 * vencido al de hoy; sin contactar, los que llevan más tiempo esperando; en contacto, por próximo contacto (sin
	 * fecha al final).
	 */
	@Transactional(readOnly = true)
	public Page<CasoResumenResponse> misCasos(FiltroMisCasos filtro, Pageable paginacion) {
		Optional<Long> propio = ambito.consolidador();
		if (propio.isEmpty()) {
			return Page.empty(paginacion);
		}
		Specification<CasoConsolidacion> criterio = Specification.allOf(misAbiertos(propio.get()),
				EspecificacionesConsolidacion.enSeccion(filtro.seccion(), hoy()),
				EspecificacionesConsolidacion.dePersonas(personasQueCoinciden(filtro.q())));
		return pagina(criterio, paginacion, ordenDe(filtro.seccion()));
	}

	@Transactional(readOnly = true)
	public ResumenMisCasosResponse resumenMisCasos() {
		Optional<Long> propio = ambito.consolidador();
		if (propio.isEmpty()) {
			return new ResumenMisCasosResponse(0, 0, 0, 0, 0);
		}
		Specification<CasoConsolidacion> abiertos = misAbiertos(propio.get());
		LocalDate hoy = hoy();
		Specification<CasoConsolidacion> vigentes = EspecificacionesConsolidacion.deConsolidador(propio.get())
			.and(EspecificacionesConsolidacion.vigentes());
		return new ResumenMisCasosResponse(
				casos.count(abiertos.and(EspecificacionesConsolidacion.enSeccion(SeccionMisCasos.PARA_HOY, hoy))),
				casos.count(abiertos.and(EspecificacionesConsolidacion.enSeccion(SeccionMisCasos.SIN_CONTACTAR, hoy))),
				casos.count(abiertos.and(EspecificacionesConsolidacion.enSeccion(SeccionMisCasos.EN_CONTACTO, hoy))),
				casos.count(vigentes), casos.count(vigentes.and(EspecificacionesConsolidacion.conRespuesta())));
	}

	@Transactional(readOnly = true)
	public CasoDetalleResponse detalle(Long id) {
		return detalleDe(visible(id));
	}

	/** Seguimiento individual. El consolidador por defecto es quien lo abre; solo el coordinador elige a otro. */
	@Transactional
	public CasoDetalleResponse abrir(AbrirSeguimientoRequest solicitud) {
		Long miembroId = solicitud.miembroId();
		if (!ambito.puedeVerPersona(miembroId) && !ambito.puedeIniciarSeguimiento(miembroId)) {
			throw new RecursoNoEncontradoException(EquipoConsolidacionService.MIEMBRO_NO_ENCONTRADO);
		}
		if (!ambito.puedeIniciarSeguimiento(miembroId)) {
			throw new AccessDeniedException(NO_PUEDE_INICIAR);
		}
		equipo.personaActiva(miembroId);
		Long consolidadorId = consolidadorPara(solicitud.consolidadorId());
		verificarSinCasoAbierto(miembroId);
		CasoConsolidacion caso = casos
			.save(CasoConsolidacion.individual(miembroId, consolidadorId, usuario(), Instant.now(reloj)));
		LOG.info("Seguimiento individual abierto casoId={} miembroId={} consolidadorId={} por usuarioId={}",
				caso.getId(), miembroId, consolidadorId, usuario());
		return detalleDe(caso);
	}

	@Transactional
	public CasoDetalleResponse cerrar(Long id, CerrarCasoRequest solicitud) {
		CasoConsolidacion caso = paraActuar(id);
		if (!caso.getEstado().abierto()) {
			throw new ConflictoException(YA_CERRADO);
		}
		ResultadoConsolidacion resultado = resultados.activo(solicitud.resultadoId());
		caso.cerrar(resultado.getId(), Textos.recortarOpcional(solicitud.observacion()), usuario(),
				Instant.now(reloj));
		LOG.info("Caso de consolidación cerrado casoId={} resultadoId={} por usuarioId={}", id, resultado.getId(),
				usuario());
		return detalleDe(caso);
	}

	/** Deshace un cierre (por error de registro): vuelve a «en contacto» o a «pendiente». */
	@Transactional
	public CasoDetalleResponse reabrir(Long id) {
		CasoConsolidacion caso = paraActuar(id);
		if (caso.getEstado().abierto()) {
			throw new ConflictoException(NO_CERRADO);
		}
		casos.findFirstByMiembroIdAndEstadoNot(caso.getMiembroId(), EstadoCaso.CERRADO)
			.ifPresent(this::lanzarCasoAbierto);
		List<ContactoConsolidacion> propios = resumenes.contactosDe(List.of(id)).getOrDefault(id, List.of());
		caso.reabrir(!propios.isEmpty(), propios.isEmpty() ? null : propios.getFirst().getProximoContacto());
		LOG.info("Caso de consolidación reabierto casoId={} por usuarioId={}", id, usuario());
		return detalleDe(caso);
	}

	/** Caso visible para el usuario; si no existe o está fuera de su ámbito, 404. */
	CasoConsolidacion visible(Long id) {
		return casos.findById(id).filter(ambito::puedeVer).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	/**
	 * Caso sobre el que el usuario puede actuar (registrar contactos, cerrar, reabrir): visible, propio o del
	 * coordinador (si no, 403) y de una ronda en curso o individual (si no, 409).
	 */
	CasoConsolidacion paraActuar(Long id) {
		CasoConsolidacion caso = visible(id);
		if (!ambito.puedeActuar(caso)) {
			throw new AccessDeniedException(NO_PUEDE_ACTUAR);
		}
		if (!trabajable(caso)) {
			throw new ConflictoException(RONDA_NO_EN_CURSO);
		}
		return caso;
	}

	private boolean trabajable(CasoConsolidacion caso) {
		return caso.esIndividual() || rondas.buscar(caso.getRondaId()).getEstado().enCurso();
	}

	private CasoDetalleResponse detalleDe(CasoConsolidacion caso) {
		Long miembroId = caso.getMiembroId();
		ContactoPersona persona = miembros.contacto(miembroId)
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
		List<CasoConsolidacion> anteriores = ResumenesDeCasos.anteriores(caso,
				casos.findByMiembroIdOrderByCreadoEnDescIdDesc(miembroId));
		List<Long> ids = new ArrayList<>(anteriores.stream().map(CasoConsolidacion::getId).toList());
		ids.add(caso.getId());
		Map<Long, List<ContactoConsolidacion>> contactosPorCaso = resumenes.contactosDe(ids);
		List<ContactoConsolidacion> propios = contactosPorCaso.getOrDefault(caso.getId(), List.of());
		Map<Long, ResultadoConsolidacion> resultadosPorId = resultados.todosPorId();
		RondaConsolidacion ronda = caso.esIndividual() ? null : rondas.buscar(caso.getRondaId());
		var personas = miembros.porIds(consolidadoresDe(caso, propios));
		LocalDate desde = LocalDate.ofInstant(caso.getCreadoEn(), reloj.getZone());
		LocalDate hasta = caso.getCerradoEn() == null ? hoy() : LocalDate.ofInstant(caso.getCerradoEn(), reloj.getZone());
		boolean puedeActuar = ambito.puedeActuar(caso) && trabajable(caso);
		return new CasoDetalleResponse(caso.getId(), mapper.aReferencia(ronda), ronda == null ? null : ronda.getEstado(),
				caso.getEstado(), mapper.aReferencia(personas.get(caso.getConsolidadorId())),
				mapper.aReferencia(resultadosPorId.get(caso.getResultadoId())), caso.getObservacionCierre(),
				caso.getProximoContacto(), caso.getCreadoEn(), caso.getCerradoEn(), aContacto(persona),
				antes(persona, anteriores, contactosPorCaso, resultadosPorId), resumenes.contactos(propios, personas),
				asistencia.reunionesEntre(miembroId, desde, hasta), asistencia.resumen(miembroId),
				resumenes.iteraciones(anteriores), puedeActuar, ambito.esCoordinador() && caso.getEstado().abierto());
	}

	/**
	 * «Antes de contactar»: número de iteración, contactos y resultados anteriores y cuántas veces asistió desde el
	 * último contacto de la iteración anterior.
	 */
	private AntesDeContactarResponse antes(ContactoPersona persona, List<CasoConsolidacion> anteriores,
			Map<Long, List<ContactoConsolidacion>> contactosPorCaso, Map<Long, ResultadoConsolidacion> resultadosPorId) {
		List<ContactoConsolidacion> previos = anteriores.stream()
			.flatMap(anterior -> contactosPorCaso.getOrDefault(anterior.getId(), List.of()).stream())
			.toList();
		long asistenciasDesde = previos.stream()
			.map(ContactoConsolidacion::getFechaHora)
			.max(Instant::compareTo)
			.map(ultimo -> LocalDate.ofInstant(ultimo, reloj.getZone()).plusDays(1))
			.map(desde -> asistencia.fechasDesde(List.of(persona.id()), desde)
				.getOrDefault(persona.id(), List.of())
				.size())
			.orElse(0)
			.longValue();
		return new AntesDeContactarResponse(anteriores.size() + 1, mapper.porMedio(previos),
				anteriores.stream()
					.filter(anterior -> anterior.getEstado() == EstadoCaso.CERRADO)
					.map(anterior -> resumenes.aAnterior(anterior, resultadosPorId))
					.toList(),
				asistenciasDesde, persona.peticion());
	}

	private ContactoPersonaResponse aContacto(ContactoPersona persona) {
		String grupo = Optional.ofNullable(persona.grupoId())
			.flatMap(grupos::buscar)
			.map(GrupoAgenda::nombre)
			.orElse(null);
		return new ContactoPersonaResponse(persona.id(), persona.nombreCompleto(), persona.telefono(),
				persona.correo(), persona.direccion(), persona.ubicacion(), persona.condicion(), persona.fechaIngreso(),
				persona.fechaNacimiento(), persona.invitadoPor(), persona.lider(), grupo, persona.peticion(),
				persona.activo());
	}

	private Long consolidadorPara(Long elegido) {
		Optional<Long> propio = usuarioActual.miembroId();
		if (elegido == null || propio.filter(elegido::equals).isPresent()) {
			return propio.orElseThrow(() -> new ReglaNegocioException(SIN_CONSOLIDADOR, CAMPO_CONSOLIDADOR));
		}
		if (!ambito.esCoordinador()) {
			throw new AccessDeniedException(SOLO_COORDINADOR_ELIGE);
		}
		return equipo.personaActiva(elegido).id();
	}

	private void verificarSinCasoAbierto(Long miembroId) {
		casos.findFirstByMiembroIdAndEstadoNot(miembroId, EstadoCaso.CERRADO).ifPresent(this::lanzarCasoAbierto);
	}

	private void lanzarCasoAbierto(CasoConsolidacion abierto) {
		if (abierto.esIndividual()) {
			throw new ConflictoException(CASO_ABIERTO_INDIVIDUAL);
		}
		throw new ConflictoException(CASO_ABIERTO_EN_RONDA.formatted(rondas.buscar(abierto.getRondaId()).getNombre()));
	}

	private Specification<CasoConsolidacion> misAbiertos(Long consolidadorId) {
		return EspecificacionesConsolidacion.deConsolidador(consolidadorId)
			.and(EspecificacionesConsolidacion.abiertosVisibles());
	}

	private Page<CasoResumenResponse> pagina(Specification<CasoConsolidacion> criterio, Pageable paginacion,
			Sort orden) {
		Page<CasoConsolidacion> pagina = casos.findAll(criterio,
				PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize(), orden));
		List<CasoResumenResponse> contenido = resumenes.resumir(pagina.getContent());
		return new PageImpl<>(contenido, pagina.getPageable(),
				pagina.getTotalElements());
	}

	private static Sort ordenDe(SeccionMisCasos seccion) {
		return switch (seccion) {
			case PARA_HOY, EN_CONTACTO -> Sort.by("proximoContacto", "id");
			case SIN_CONTACTAR -> Sort.by("creadoEn", "id");
		};
	}

	private Set<Long> personasQueCoinciden(String q) {
		return q == null || q.isBlank() ? null : miembros.idsQueCoinciden(q);
	}

	private static List<Long> consolidadoresDe(CasoConsolidacion caso, List<ContactoConsolidacion> propios) {
		List<Long> ids = new ArrayList<>(propios.stream().map(ContactoConsolidacion::getConsolidadorId).toList());
		ids.add(caso.getConsolidadorId());
		return ids;
	}

	private LocalDate hoy() {
		return LocalDate.now(reloj);
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
