package com.iglesia.api.asistencia.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;
import com.iglesia.api.asistencia.dto.AbrirReunionRequest;
import com.iglesia.api.asistencia.dto.CrearEventoRequest;
import com.iglesia.api.asistencia.dto.FiltroReuniones;
import com.iglesia.api.asistencia.dto.ReunionHoyResponse;
import com.iglesia.api.asistencia.dto.ReunionResponse;
import com.iglesia.api.asistencia.entity.CultoProgramado;
import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.ReunionAbierta;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.model.TotalPorReunion;
import com.iglesia.api.asistencia.repository.AsistenciaRepository;
import com.iglesia.api.asistencia.repository.CultoProgramadoRepository;
import com.iglesia.api.asistencia.repository.EspecificacionesReunion;
import com.iglesia.api.asistencia.repository.ReunionRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.grupos.model.DiaSemana;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.service.AgendaGrupos;

/**
 * Reuniones de la iglesia del token: las de hoy, la apertura de las de cultos y grupos (se crean la primera vez que
 * alguien toma asistencia ese día, sin tareas programadas), los eventos y el historial. Todo respeta el ámbito de
 * {@link AmbitoAsistencia}; una reunión fuera de él responde 404, igual que si no existiera.
 */
@Service
public class ReunionService {

	private static final Logger LOG = LoggerFactory.getLogger(ReunionService.class);

	static final String NO_ENCONTRADA = "Reunión no encontrada";

	static final String GRUPO_NO_ENCONTRADO = "Grupo no encontrado";

	static final String EVENTO_LARGO = "El evento dura como mucho " + LimitesAsistencia.DIAS_EVENTO_MAX
			+ " días: la fecha de fin debe estar entre la de inicio y " + (LimitesAsistencia.DIAS_EVENTO_MAX - 1)
			+ " días después";

	static final String RANGO_INVALIDO = "La fecha final no puede ser anterior a la inicial";

	static final String SOLO_EVENTOS = "Solo se pueden eliminar eventos";

	static final String EVENTO_CON_ASISTENTES = "El evento ya tiene asistentes y no se puede eliminar";

	/** Por hora (las que no tienen, al final) y luego por nombre. */
	private static final Comparator<ReunionHoyResponse> POR_HORA = Comparator
		.comparing(ReunionHoyResponse::hora, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(ReunionHoyResponse::nombre);

	private final ReunionRepository reuniones;

	private final AsistenciaRepository asistencias;

	private final CultoProgramadoRepository cultos;

	private final CultoProgramadoService horarios;

	private final AgendaGrupos grupos;

	private final AmbitoAsistencia ambito;

	private final AsistenciaMapper mapper;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	ReunionService(ReunionRepository reuniones, AsistenciaRepository asistencias, CultoProgramadoRepository cultos,
			CultoProgramadoService horarios, AgendaGrupos grupos, AmbitoAsistencia ambito, AsistenciaMapper mapper,
			IglesiaActual iglesiaActual, UsuarioActual usuarioActual, Clock reloj) {
		this.reuniones = reuniones;
		this.asistencias = asistencias;
		this.cultos = cultos;
		this.horarios = horarios;
		this.grupos = grupos;
		this.ambito = ambito;
		this.mapper = mapper;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/**
	 * Reuniones de hoy donde el usuario puede marcar: las ya abiertas (con su total) y, por abrir, los cultos activos
	 * del día y los grupos activos que se reúnen hoy. Un culto movido a hoy aparece porque ya se abrió.
	 */
	@Transactional(readOnly = true)
	public List<ReunionHoyResponse> hoy() {
		AlcanceAsistencia alcance = ambito.paraMarcar();
		if (alcance.vacio()) {
			return List.of();
		}
		LocalDate fecha = hoyEnLaIglesia();
		DiaSemana dia = DiaSemana.de(fecha.getDayOfWeek());
		List<Reunion> abiertas = reuniones.findByFecha(fecha)
			.stream()
			.filter(reunion -> alcance.incluye(reunion.getTipo(), reunion.getGrupoId()))
			.toList();
		Map<Long, Long> totales = totales(abiertas.stream().map(Reunion::getId).toList());
		Set<Long> cultosAbiertos = abiertas.stream().map(Reunion::getCultoProgramadoId).collect(Collectors.toSet());
		Set<Long> gruposAbiertos = abiertas.stream().map(Reunion::getGrupoId).collect(Collectors.toSet());

		List<ReunionHoyResponse> resultado = new ArrayList<>();
		abiertas.forEach(reunion -> resultado.add(mapper.aHoy(reunion, totales.getOrDefault(reunion.getId(), 0L))));
		if (alcance.todas()) {
			cultos.findByDiaSemanaAndActivoTrue(dia)
				.stream()
				.filter(culto -> !cultosAbiertos.contains(culto.getId()))
				.forEach(culto -> resultado.add(mapper.aHoyPorAbrir(culto)));
		}
		grupos.activosDelDia(dia)
			.stream()
			.filter(grupo -> alcance.incluyeGrupo(grupo.id()) && !gruposAbiertos.contains(grupo.id()))
			.forEach(grupo -> resultado.add(mapper.aHoyPorAbrir(grupo)));
		resultado.sort(POR_HORA);
		return resultado;
	}

	/**
	 * Abre la reunión del culto o del grupo en esa fecha, o devuelve la que ya existe. Solo quien marca en toda la
	 * iglesia abre cultos; el líder, solo sus grupos (si no, 404).
	 */
	@Transactional
	public ReunionAbierta abrir(AbrirReunionRequest solicitud) {
		AlcanceAsistencia alcance = ambito.paraMarcar();
		return solicitud.cultoProgramadoId() != null ? abrirCulto(solicitud.cultoProgramadoId(), solicitud.fecha(), alcance)
				: abrirGrupo(solicitud.grupoId(), solicitud.fecha(), alcance);
	}

	/** Crea un evento: una reunión por día, de la fecha de inicio a la de fin (hasta una semana). */
	@Transactional
	public List<ReunionResponse> crearEvento(CrearEventoRequest solicitud) {
		LocalDate fin = solicitud.fechaFin() == null ? solicitud.fechaInicio() : solicitud.fechaFin();
		long dias = ChronoUnit.DAYS.between(solicitud.fechaInicio(), fin);
		if (dias < 0 || dias >= LimitesAsistencia.DIAS_EVENTO_MAX) {
			throw new ReglaNegocioException(EVENTO_LARGO, "fechaFin");
		}
		String nombre = Textos.normalizar(solicitud.nombre());
		Instant ahora = Instant.now(reloj);
		List<Reunion> creadas = solicitud.fechaInicio()
			.datesUntil(fin.plusDays(1))
			.map(fecha -> Reunion.evento(nombre, fecha, solicitud.hora(), usuario(), ahora))
			.toList();
		creadas = reuniones.saveAll(creadas);
		LOG.info("Evento creado con {} reuniones (primera id={}) por usuarioId={}", creadas.size(),
				creadas.getFirst().getId(), usuario());
		return creadas.stream().map(reunion -> mapper.aRespuesta(reunion, 0)).toList();
	}

	/** Elimina un evento creado por error; solo si es un evento y no tiene asistentes (409). */
	@Transactional
	public void eliminarEvento(Long id) {
		Reunion reunion = reuniones.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
		if (reunion.getTipo() != TipoReunion.EVENTO) {
			throw new ConflictoException(SOLO_EVENTOS);
		}
		if (asistencias.existsByReunionId(id)) {
			throw new ConflictoException(EVENTO_CON_ASISTENTES);
		}
		reuniones.delete(reunion);
		LOG.info("Evento eliminado reunionId={} por usuarioId={}", id, usuario());
	}

	/** Historial paginado con el total de cada reunión, contado en un solo lote. */
	@Transactional(readOnly = true)
	public Page<ReunionResponse> historial(FiltroReuniones filtro, Pageable paginacion) {
		if (filtro.desde() != null && filtro.hasta() != null && filtro.hasta().isBefore(filtro.desde())) {
			throw new ReglaNegocioException(RANGO_INVALIDO, "hasta");
		}
		Specification<Reunion> criterio = Specification.allOf(EspecificacionesReunion.dentroDe(ambito.paraConsultar()),
				EspecificacionesReunion.desde(filtro.desde()), EspecificacionesReunion.hasta(filtro.hasta()),
				EspecificacionesReunion.deTipo(filtro.tipo()), EspecificacionesReunion.deGrupo(filtro.grupoId()));
		Page<Reunion> pagina = reuniones.findAll(criterio, paginacion);
		Map<Long, Long> totales = totales(pagina.getContent().stream().map(Reunion::getId).toList());
		return pagina.map(reunion -> mapper.aRespuesta(reunion, totales.getOrDefault(reunion.getId(), 0L)));
	}

	@Transactional(readOnly = true)
	public ReunionResponse obtener(Long id) {
		Reunion reunion = paraVer(id);
		return mapper.aRespuesta(reunion, asistencias.countByReunionId(id));
	}

	/** Reunión donde el usuario puede marcar (404 si no existe o está fuera de su ámbito). */
	Reunion paraMarcar(Long id) {
		return dentroDe(id, ambito.paraMarcar());
	}

	/** Reunión que el usuario puede ver con sus asistentes (404 si no existe o está fuera de su ámbito). */
	Reunion paraVer(Long id) {
		return dentroDe(id, ambito.paraVerReunion());
	}

	/** Hoy según el reloj del servidor, que corre en la zona horaria de la iglesia. */
	LocalDate hoyEnLaIglesia() {
		return LocalDate.now(reloj);
	}

	private ReunionAbierta abrirCulto(Long cultoId, LocalDate fecha, AlcanceAsistencia alcance) {
		if (!alcance.todas()) {
			throw new RecursoNoEncontradoException(CultoProgramadoService.NO_ENCONTRADO);
		}
		CultoProgramado culto = horarios.buscar(cultoId);
		int creada = reuniones.abrirDeCulto(iglesiaActual.requerida(), cultoId, culto.getNombre(), fecha,
				culto.getHora(), usuario(), Instant.now(reloj));
		Reunion reunion = reuniones.findByCultoProgramadoIdAndFecha(cultoId, fecha).orElseThrow();
		return abierta(reunion, creada == 1);
	}

	private ReunionAbierta abrirGrupo(Long grupoId, LocalDate fecha, AlcanceAsistencia alcance) {
		GrupoAgenda grupo = grupos.buscar(grupoId)
			.filter(encontrado -> alcance.incluyeGrupo(encontrado.id()))
			.orElseThrow(() -> new RecursoNoEncontradoException(GRUPO_NO_ENCONTRADO));
		LocalTime hora = grupo.horaReunion();
		int creada = reuniones.abrirDeGrupo(iglesiaActual.requerida(), grupoId, grupo.nombre(), fecha, hora, usuario(),
				Instant.now(reloj));
		Reunion reunion = reuniones.findByGrupoIdAndFecha(grupoId, fecha).orElseThrow();
		return abierta(reunion, creada == 1);
	}

	private ReunionAbierta abierta(Reunion reunion, boolean creada) {
		if (creada) {
			LOG.info("Reunión abierta id={} tipo={} por usuarioId={}", reunion.getId(), reunion.getTipo(), usuario());
		}
		return new ReunionAbierta(mapper.aRespuesta(reunion, asistencias.countByReunionId(reunion.getId())), creada);
	}

	private Reunion dentroDe(Long id, AlcanceAsistencia alcance) {
		return reuniones.findById(id)
			.filter(reunion -> alcance.incluye(reunion.getTipo(), reunion.getGrupoId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
	}

	private Map<Long, Long> totales(Collection<Long> reunionIds) {
		if (reunionIds.isEmpty()) {
			return Map.of();
		}
		return asistencias.contarPorReunion(reunionIds)
			.stream()
			.collect(Collectors.toMap(TotalPorReunion::reunionId, TotalPorReunion::total));
	}

	/** Toda petición que escribe llega autenticada. */
	private Long usuario() {
		return usuarioActual.id().orElseThrow(() -> new IllegalStateException("Petición sin usuario autenticado"));
	}

}
