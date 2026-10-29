package com.iglesia.api.grupos.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.grupos.dto.CandidatoGrupoResponse;
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.dto.GuardarGrupoRequest;
import com.iglesia.api.grupos.dto.IntegranteGrupoResponse;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.mapper.GrupoMapper;
import com.iglesia.api.grupos.model.DatosGrupo;
import com.iglesia.api.grupos.repository.GrupoRepository;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.miembros.service.ConsultaGrupos;
import com.iglesia.api.miembros.service.ReferenciasMiembro;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

/**
 * Mantenimiento de los grupos de la iglesia del token y de sus integrantes. Un grupo no tiene nombre: es el grupo de
 * su líder, que debe ser un miembro activo y no tener otro grupo. Un grupo no se borra: se desactiva, si no tiene
 * miembros activos.
 */
@Service
public class GrupoService {

	private static final Logger LOG = LoggerFactory.getLogger(GrupoService.class);

	static final String NO_ENCONTRADO = "Grupo no encontrado";

	static final String LIDER_CON_GRUPO = "Ese líder ya tiene un grupo";

	static final String CON_MIEMBROS = "No se puede desactivar: el grupo tiene miembros activos. Cámbielos de grupo "
			+ "antes";

	static final String GRUPO_INACTIVO = "El grupo está desactivado: reactívelo para agregarle miembros";

	private static final String CAMPO_LIDER = "liderId";

	/** Orden alfabético en español, sin distinguir mayúsculas ni tildes. */
	private static final Collator ALFABETICO = Collator.getInstance(Locale.of("es"));

	private final GrupoRepository grupos;

	private final GrupoMapper mapper;

	private final ReferenciasMiembro miembros;

	private final ConsultaGrupos consulta;

	private final UbigeoService ubigeo;

	private final UsuarioActual usuarioActual;

	GrupoService(GrupoRepository grupos, GrupoMapper mapper, ReferenciasMiembro miembros, ConsultaGrupos consulta,
			UbigeoService ubigeo, UsuarioActual usuarioActual) {
		this.grupos = grupos;
		this.mapper = mapper;
		this.miembros = miembros;
		this.consulta = consulta;
		this.ubigeo = ubigeo;
		this.usuarioActual = usuarioActual;
	}

	/** Todos los grupos, ordenados por el nombre de su líder. */
	@Transactional(readOnly = true)
	public List<GrupoResponse> listar() {
		return aRespuestas(grupos.findAll())
			.stream()
			.sorted(Comparator.comparing(grupo -> grupo.lider().nombre(), ALFABETICO))
			.toList();
	}

	@Transactional(readOnly = true)
	public GrupoResponse obtener(Long id) {
		return aRespuestas(List.of(buscar(id))).getFirst();
	}

	@Transactional
	public GrupoResponse crear(GuardarGrupoRequest solicitud) {
		if (grupos.existsByLiderId(solicitud.liderId())) {
			throw new ConflictoException(LIDER_CON_GRUPO, CAMPO_LIDER);
		}
		Grupo grupo = guardar(Grupo.nuevo(datos(null, solicitud)));
		LOG.info("Grupo creado id={} por usuarioId={}", grupo.getId(), usuarioActual.id().orElse(null));
		return obtener(grupo.getId());
	}

	@Transactional
	public GrupoResponse actualizar(Long id, GuardarGrupoRequest solicitud) {
		Grupo grupo = buscar(id);
		if (grupos.existsByLiderIdAndIdNot(solicitud.liderId(), id)) {
			throw new ConflictoException(LIDER_CON_GRUPO, CAMPO_LIDER);
		}
		grupo.actualizar(datos(grupo, solicitud));
		guardar(grupo);
		LOG.info("Grupo actualizado id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return obtener(id);
	}

	@Transactional
	public GrupoResponse cambiarEstado(Long id, boolean activo) {
		Grupo grupo = buscar(id);
		if (!activo && grupo.isActivo() && miembros.hayActivosEnGrupo(id)) {
			throw new ConflictoException(CON_MIEMBROS);
		}
		grupo.cambiarEstado(activo);
		LOG.info("Grupo {} id={} por usuarioId={}", activo ? "reactivado" : "desactivado", id,
				usuarioActual.id().orElse(null));
		return obtener(id);
	}

	@Transactional(readOnly = true)
	public List<IntegranteGrupoResponse> integrantes(Long id) {
		Grupo grupo = buscar(id);
		return miembros.integrantesDe(grupo.getId()).stream().map(mapper::aIntegrante).toList();
	}

	/** Miembros activos que se pueden agregar al grupo, con el grupo en el que están ahora. */
	@Transactional(readOnly = true)
	public List<CandidatoGrupoResponse> candidatos(Long id, String q) {
		Grupo grupo = buscar(id);
		List<IntegranteGrupo> candidatos = miembros.candidatosPara(grupo.getId(), q);
		Map<Long, ReferenciaResponse> actuales = consulta
			.referencias(candidatos.stream().map(IntegranteGrupo::grupoId).toList());
		return candidatos.stream()
			.map(candidato -> mapper.aCandidato(candidato,
					candidato.grupoId() == null ? null : actuales.get(candidato.grupoId())))
			.toList();
	}

	/** Agrega el miembro al grupo o lo mueve desde otro. */
	@Transactional
	public IntegranteGrupoResponse agregarIntegrante(Long id, Long miembroId) {
		Grupo grupo = buscar(id);
		if (!grupo.isActivo()) {
			throw new ConflictoException(GRUPO_INACTIVO);
		}
		IntegranteGrupo antes = miembros.ponerEnGrupo(miembroId, id);
		LOG.info("Miembro id={} agregado al grupo id={} (antes en grupoId={}) por usuarioId={}", miembroId, id,
				antes.grupoId(), usuarioActual.id().orElse(null));
		return mapper.aIntegrante(antes);
	}

	@Transactional
	public void quitarIntegrante(Long id, Long miembroId) {
		buscar(id);
		miembros.quitarDeGrupo(miembroId, id);
		LOG.info("Miembro id={} quitado del grupo id={} por usuarioId={}", miembroId, id,
				usuarioActual.id().orElse(null));
	}

	private DatosGrupo datos(Grupo grupo, GuardarGrupoRequest solicitud) {
		miembros.validarAsignable(solicitud.liderId(), grupo == null ? null : grupo.getLiderId(), CAMPO_LIDER);
		miembros.validarAsignable(solicitud.anfitrionId(), grupo == null ? null : grupo.getAnfitrionId(),
				"anfitrionId");
		String distrito = Textos.normalizarOpcional(solicitud.distritoCodigo());
		ubigeo.validarDistrito(distrito, "distritoCodigo");
		return new DatosGrupo(solicitud.liderId(), solicitud.anfitrionId(), solicitud.diaReunion(),
				solicitud.horaReunion(), Textos.normalizarOpcional(solicitud.direccion()), distrito,
				Textos.normalizarOpcional(solicitud.descripcion()));
	}

	/** Respuestas con líder, anfitrión, ubicación y conteo resueltos por lotes (sin N+1). */
	private List<GrupoResponse> aRespuestas(List<Grupo> lista) {
		Map<Long, ReferenciaResponse> personas = miembros
			.referencias(lista.stream().flatMap(g -> Stream.of(g.getLiderId(), g.getAnfitrionId())).toList());
		Map<Long, Long> conteos = miembros.activosPorGrupo(lista.stream().map(Grupo::getId).toList());
		Map<String, UbicacionResponse> ubicaciones = ubigeo
			.ubicaciones(lista.stream().map(Grupo::getDistritoCodigo).toList());
		return lista.stream()
			.map(grupo -> mapper.aRespuesta(grupo, personas.get(grupo.getLiderId()),
					personas.get(grupo.getAnfitrionId()), ubicaciones.get(grupo.getDistritoCodigo()),
					conteos.getOrDefault(grupo.getId(), 0L)))
			.toList();
	}

	private Grupo buscar(Long id) {
		return grupos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	/** Escribe ya para que un líder repetido por un alta simultánea (índice único) responda 409 y no 500. */
	private Grupo guardar(Grupo grupo) {
		try {
			return grupos.saveAndFlush(grupo);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(LIDER_CON_GRUPO, CAMPO_LIDER);
		}
	}

}
