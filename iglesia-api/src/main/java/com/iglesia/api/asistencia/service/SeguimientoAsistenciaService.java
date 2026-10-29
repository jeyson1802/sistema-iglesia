package com.iglesia.api.asistencia.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;
import com.iglesia.api.asistencia.constant.LimitesSeguimiento;
import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.ConteoAlertasResponse;
import com.iglesia.api.asistencia.dto.FiltroOcasiones;
import com.iglesia.api.asistencia.dto.FiltroSeguimiento;
import com.iglesia.api.asistencia.dto.OcasionResponse;
import com.iglesia.api.asistencia.dto.PersonaSeguimientoResponse;
import com.iglesia.api.asistencia.dto.SeguimientoGrupoResponse;
import com.iglesia.api.asistencia.dto.SemanaAsistenciaResponse;
import com.iglesia.api.asistencia.dto.SemanaGrupoResponse;
import com.iglesia.api.asistencia.dto.SerieCultoResponse;
import com.iglesia.api.asistencia.dto.ServiciosSeguimientoResponse;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.mapper.SeguimientoMapper;
import com.iglesia.api.asistencia.model.AlcanceSeguimiento;
import com.iglesia.api.asistencia.model.Asistidas;
import com.iglesia.api.asistencia.model.MiembroEvaluado;
import com.iglesia.api.asistencia.model.ReglasSeguimiento;
import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.asistencia.model.TipoReunion;
import com.iglesia.api.asistencia.repository.ReunionRepository;
import com.iglesia.api.asistencia.repository.SeguimientoRepository;
import com.iglesia.api.asistencia.repository.SeguimientoRepository.FilaSemana;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.grupos.model.GrupoAgenda;
import com.iglesia.api.grupos.service.AgendaGrupos;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;

/**
 * Seguimiento de asistencia: qué ocasiones le corresponden a cada miembro (días de culto y reuniones de su grupo),
 * a cuáles asistió, su evolución y sus alertas, por miembro, por grupo, para la lista de seguimiento y por servicio.
 * Los indicadores se calculan en la base para todos los miembros de la consulta a la vez; las alertas, aquí, con las
 * reglas de la iglesia. Nada se guarda: siempre reflejan la última marca.
 */
@Service
public class SeguimientoAsistenciaService {

	static final String MIEMBRO_NO_ENCONTRADO = "Miembro no encontrado";

	static final String GRUPO_NO_ENCONTRADO = "Grupo no encontrado";

	private final SeguimientoRepository consultas;

	private final ReunionRepository reuniones;

	private final ConfiguracionSeguimientoService configuracion;

	private final MiembrosParaAsistencia miembros;

	private final AgendaGrupos grupos;

	private final AmbitoAsistencia ambito;

	private final SeguimientoMapper mapper;

	private final AsistenciaMapper asistenciaMapper;

	private final UsuarioActual usuarioActual;

	private final IglesiaActual iglesiaActual;

	private final Clock reloj;

	SeguimientoAsistenciaService(SeguimientoRepository consultas, ReunionRepository reuniones,
			ConfiguracionSeguimientoService configuracion, MiembrosParaAsistencia miembros, AgendaGrupos grupos,
			AmbitoAsistencia ambito, SeguimientoMapper mapper, AsistenciaMapper asistenciaMapper,
			UsuarioActual usuarioActual, IglesiaActual iglesiaActual, Clock reloj) {
		this.consultas = consultas;
		this.reuniones = reuniones;
		this.configuracion = configuracion;
		this.miembros = miembros;
		this.grupos = grupos;
		this.ambito = ambito;
		this.mapper = mapper;
		this.asistenciaMapper = asistenciaMapper;
		this.usuarioActual = usuarioActual;
		this.iglesiaActual = iglesiaActual;
		this.reloj = reloj;
	}

	/** Resumen de la ficha. Un miembro inactivo no tiene ocasiones: viene sin datos ni alertas. */
	@Transactional(readOnly = true)
	public AsistenciaMiembroResponse delMiembro(Long miembroId) {
		verificarMiembroVisible(miembroId);
		Periodo periodo = periodo();
		var evaluado = evaluados(periodo, miembroId, null, null).stream().findFirst();
		var semanas = semanasDe(periodo, consultas.porSemana(iglesiaActual.requerida(), periodo.hoy(),
				periodo.desdeAnalisis(), miembroId, null, null, false));
		var ultimas = reuniones.ultimasAsistidas(miembroId, PageRequest.of(0, LimitesAsistencia.ULTIMAS_REUNIONES))
			.stream()
			.map(asistenciaMapper::aAsistida)
			.toList();
		Asistidas ultimas8 = evaluado.map(e -> e.indicadores().ultimas8()).orElse(new Asistidas(0, 0));
		return new AsistenciaMiembroResponse(ultimas8.esperadas(), ultimas8.asistidas(), ultimas8.porcentaje(),
				evaluado.map(e -> e.indicadores().semanas9a16().porcentaje()).orElse(null),
				evaluado.map(e -> e.indicadores().faltasSeguidas()).orElse(0),
				evaluado.map(e -> e.indicadores().ultimaAsistencia()).orElse(null),
				evaluado.map(MiembroEvaluado::alertas).orElse(List.of()), semanas, ultimas);
	}

	@Transactional(readOnly = true)
	public Page<OcasionResponse> ocasiones(Long miembroId, FiltroOcasiones filtro, Pageable paginacion) {
		verificarMiembroVisible(miembroId);
		LocalDate hoy = LocalDate.now(reloj);
		LocalDate hasta = filtro.hasta() == null ? hoy : filtro.hasta();
		if (filtro.desde() != null && hasta.isBefore(filtro.desde())) {
			throw new ReglaNegocioException(ReunionService.RANGO_INVALIDO);
		}
		String estado = filtro.estado() == null ? null : filtro.estado().name();
		return consultas
			.ocasionesDelMiembro(iglesiaActual.requerida(), miembroId, hoy, filtro.desde(), hasta, estado,
					PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize()))
			.map(mapper::aOcasion);
	}

	/** Lista de seguimiento del alcance del usuario, de mayor a menor riesgo, paginada. */
	@Transactional(readOnly = true)
	public Page<PersonaSeguimientoResponse> lista(FiltroSeguimiento filtro, Pageable paginacion) {
		var seleccion = delAlcance(filtro.grupoId(), filtro.q()).stream()
			.filter(evaluado -> filtro.incluirSinAlertas() || !evaluado.alertas().isEmpty())
			.filter(evaluado -> filtro.alerta() == null || evaluado.tiene(filtro.alerta()))
			.sorted(MiembroEvaluado.POR_RIESGO)
			.toList();
		int desde = (int) Math.min(paginacion.getOffset(), seleccion.size());
		int hasta = Math.min(desde + paginacion.getPageSize(), seleccion.size());
		return new PageImpl<>(aPersonas(seleccion.subList(desde, hasta)),
				PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize()), seleccion.size());
	}

	@Transactional(readOnly = true)
	public ConteoAlertasResponse conteos(Long grupoId, String q) {
		var evaluados = delAlcance(grupoId, q);
		return new ConteoAlertasResponse(evaluados.stream().filter(e -> !e.alertas().isEmpty()).count(),
				contar(evaluados, TipoAlerta.FALTAS_SEGUIDAS), contar(evaluados, TipoAlerta.NUEVO_SIN_VOLVER),
				contar(evaluados, TipoAlerta.ASISTENCIA_EN_CAIDA));
	}

	/** Evolución del grupo y sus integrantes por riesgo; un grupo fuera del ámbito responde 404. */
	@Transactional(readOnly = true)
	public SeguimientoGrupoResponse delGrupo(Long grupoId) {
		grupos.buscar(grupoId)
			.filter(grupo -> ambito.paraConsultar().incluyeGrupo(grupo.id()))
			.orElseThrow(() -> new RecursoNoEncontradoException(GRUPO_NO_ENCONTRADO));
		Periodo periodo = periodo();
		long iglesiaId = iglesiaActual.requerida();
		Map<Integer, FilaSemana> ocasiones = porIndice(consultas.porSemana(iglesiaId, periodo.hoy(),
				periodo.desdeAnalisis(), null, grupoId, null, true), FilaSemana::getSemana);
		var reunionesPorSemana = porIndice(
				consultas.reunionesDeGrupoPorSemana(iglesiaId, periodo.hoy(), periodo.desdeEvolucion(), grupoId),
				SeguimientoRepository.FilaReunionesSemana::getSemana);
		var semanas = periodo.indicesEvolucion().mapToObj(indice -> {
			var reunionesSemana = reunionesPorSemana.get(indice);
			var ocasion = ocasiones.get(indice);
			Integer porcentaje = ocasion == null ? null
					: new Asistidas(ocasion.getEsperadas(), ocasion.getAsistidas()).porcentaje();
			return new SemanaGrupoResponse(periodo.lunes(indice),
					reunionesSemana == null ? 0 : reunionesSemana.getReuniones(),
					reunionesSemana == null ? 0 : reunionesSemana.getAsistentes(), porcentaje);
		}).toList();
		var integrantes = evaluados(periodo, null, grupoId, null).stream().sorted(MiembroEvaluado.POR_RIESGO).toList();
		return new SeguimientoGrupoResponse(semanas, aPersonas(integrantes));
	}

	/** Evolución de cada culto, de las personas en los días de culto y de los grupos. */
	@Transactional(readOnly = true)
	public ServiciosSeguimientoResponse servicios() {
		Periodo periodo = periodo();
		long iglesiaId = iglesiaActual.requerida();
		var porCulto = consultas.asistentesPorCulto(iglesiaId, periodo.hoy(), periodo.desdeEvolucion())
			.stream()
			.collect(Collectors.groupingBy(SeguimientoRepository.FilaCulto::getCultoId));
		var cultos = porCulto.values().stream().map(filas -> {
			var porSemana = porIndice(filas, SeguimientoRepository.FilaCulto::getSemana);
			List<Long> serie = periodo.indicesEvolucion().mapToObj(indice -> {
				var fila = porSemana.get(indice);
				return fila == null ? null : fila.getAsistentes();
			}).toList();
			return new SerieCultoResponse(filas.getFirst().getCultoId(), filas.getFirst().getNombre(), serie);
		}).sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre())).toList();
		var porTipo = consultas.personasPorTipoYSemana(iglesiaId, periodo.hoy(), periodo.desdeEvolucion())
			.stream()
			.collect(Collectors.toMap(fila -> fila.getTipo() + fila.getSemana(), Function.identity()));
		Map<Integer, FilaSemana> ocasionesGrupo = porIndice(consultas.porSemana(iglesiaId, periodo.hoy(),
				periodo.desdeAnalisis(), null, null, null, true), FilaSemana::getSemana);
		List<Long> personasEnCultos = periodo.indicesEvolucion().mapToObj(indice -> {
			var fila = porTipo.get(TipoReunion.CULTO.name() + indice);
			return fila == null ? 0L : fila.getPersonas();
		}).toList();
		List<Long> presentesEnGrupos = periodo.indicesEvolucion().mapToObj(indice -> {
			var fila = porTipo.get(TipoReunion.GRUPO.name() + indice);
			return fila == null ? 0L : fila.getAsistencias();
		}).toList();
		List<Integer> porcentajeGrupos = periodo.indicesEvolucion().mapToObj(indice -> {
			var fila = ocasionesGrupo.get(indice);
			return fila == null ? null : new Asistidas(fila.getEsperadas(), fila.getAsistidas()).porcentaje();
		}).toList();
		List<LocalDate> semanas = periodo.indicesEvolucion().mapToObj(periodo::lunes).toList();
		return new ServiciosSeguimientoResponse(semanas, cultos, personasEnCultos, presentesEnGrupos,
				porcentajeGrupos);
	}

	private List<MiembroEvaluado> delAlcance(Long grupoId, String q) {
		AlcanceSeguimiento alcance = ambito.paraSeguimiento();
		if (alcance.vacio()) {
			return List.of();
		}
		var evaluados = evaluados(periodo(), null, grupoId, alcance.liderId());
		if (q == null || q.isBlank()) {
			return evaluados;
		}
		Set<Long> coinciden = new HashSet<>(miembros.idsQueCoinciden(q));
		return evaluados.stream().filter(evaluado -> coinciden.contains(evaluado.indicadores().miembroId())).toList();
	}

	private List<MiembroEvaluado> evaluados(Periodo periodo, Long miembroId, Long grupoId, Long liderId) {
		ReglasSeguimiento reglas = configuracion.reglas();
		return consultas
			.indicadores(iglesiaActual.requerida(), periodo.hoy(), periodo.desdeAnalisis(), miembroId, grupoId,
					liderId, false)
			.stream()
			.map(mapper::aIndicadores)
			.map(indicadores -> MiembroEvaluado.evaluar(indicadores, reglas, periodo.hoy()))
			.toList();
	}

	/** Completa nombre, nivel, grupo y líder solo de las personas que se devuelven. */
	private List<PersonaSeguimientoResponse> aPersonas(List<MiembroEvaluado> evaluados) {
		Collection<Long> idsMiembros = evaluados.stream()
			.flatMap(e -> Stream.of(e.indicadores().miembroId(), e.indicadores().liderId()))
			.collect(Collectors.toSet());
		Set<Long> idsGrupos = evaluados.stream()
			.map(e -> e.indicadores().grupoId())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<Long, MiembroAsistente> porMiembro = miembros.porIds(idsMiembros);
		Map<Long, GrupoAgenda> porGrupo = grupos.buscarTodos(idsGrupos)
			.stream()
			.collect(Collectors.toMap(GrupoAgenda::id, Function.identity()));
		return evaluados.stream()
			.map(e -> mapper.aPersona(e.indicadores(), e.alertas(), porMiembro, porGrupo))
			.toList();
	}

	private List<SemanaAsistenciaResponse> semanasDe(Periodo periodo, List<FilaSemana> filas) {
		Map<Integer, FilaSemana> porSemana = porIndice(filas, FilaSemana::getSemana);
		return periodo.indicesEvolucion().mapToObj(indice -> {
			FilaSemana fila = porSemana.get(indice);
			Asistidas asistidas = fila == null ? new Asistidas(0, 0)
					: new Asistidas(fila.getEsperadas(), fila.getAsistidas());
			return new SemanaAsistenciaResponse(periodo.lunes(indice), asistidas.esperadas(), asistidas.asistidas(),
					asistidas.porcentaje());
		}).toList();
	}

	/** Con {@code asistencia.ver}, cualquiera; si no, solo los miembros cuya ficha puede ver. Si no, 404. */
	private void verificarMiembroVisible(Long miembroId) {
		boolean puedeVer = usuarioActual.tienePermiso(Permisos.ASISTENCIA_VER) || miembros.visible(miembroId);
		miembros.porId(miembroId)
			.filter(encontrado -> puedeVer)
			.orElseThrow(() -> new RecursoNoEncontradoException(MIEMBRO_NO_ENCONTRADO));
	}

	private Periodo periodo() {
		return new Periodo(LocalDate.now(reloj));
	}

	private static long contar(List<MiembroEvaluado> evaluados, TipoAlerta alerta) {
		return evaluados.stream().filter(evaluado -> evaluado.tiene(alerta)).count();
	}

	private static <T> Map<Integer, T> porIndice(List<T> filas, Function<T, Integer> semana) {
		return filas.stream().collect(Collectors.toMap(semana, Function.identity()));
	}

	/**
	 * Semanas del seguimiento: la 0 es la actual (de lunes a domingo). Se analizan
	 * {@value LimitesSeguimiento#SEMANAS_ANALISIS} y la evolución muestra las últimas
	 * {@value LimitesSeguimiento#SEMANAS_EVOLUCION}.
	 */
	private record Periodo(LocalDate hoy) {

		LocalDate lunes(int indice) {
			return hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(indice);
		}

		LocalDate desdeAnalisis() {
			return lunes(LimitesSeguimiento.SEMANAS_ANALISIS - 1);
		}

		LocalDate desdeEvolucion() {
			return lunes(LimitesSeguimiento.SEMANAS_EVOLUCION - 1);
		}

		/** Índices de la evolución, de la semana más antigua a la actual. */
		IntStream indicesEvolucion() {
			return IntStream.iterate(LimitesSeguimiento.SEMANAS_EVOLUCION - 1, indice -> indice >= 0,
					indice -> indice - 1);
		}

	}

}
