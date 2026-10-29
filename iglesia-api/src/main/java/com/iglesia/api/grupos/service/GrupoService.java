package com.iglesia.api.grupos.service;

import java.util.List;
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
import com.iglesia.api.grupos.dto.GrupoResponse;
import com.iglesia.api.grupos.dto.GuardarGrupoRequest;
import com.iglesia.api.grupos.entity.Grupo;
import com.iglesia.api.grupos.mapper.GrupoMapper;
import com.iglesia.api.grupos.model.DatosGrupo;
import com.iglesia.api.grupos.repository.GrupoRepository;
import com.iglesia.api.miembros.service.ReferenciasMiembro;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

/**
 * Mantenimiento de los grupos de la iglesia del token. El nombre es único sin distinguir mayúsculas; el líder y el
 * anfitrión deben ser miembros activos al asignarlos. Un grupo no se borra: se desactiva, si no tiene miembros
 * activos.
 */
@Service
public class GrupoService {

	private static final Logger LOG = LoggerFactory.getLogger(GrupoService.class);

	static final String NO_ENCONTRADO = "Grupo no encontrado";

	static final String NOMBRE_REPETIDO = "Ya existe un grupo con ese nombre";

	static final String CON_MIEMBROS = "No se puede desactivar: el grupo tiene miembros activos. Cámbielos de grupo "
			+ "antes";

	private final GrupoRepository grupos;

	private final GrupoMapper mapper;

	private final ReferenciasMiembro miembros;

	private final UbigeoService ubigeo;

	private final UsuarioActual usuarioActual;

	GrupoService(GrupoRepository grupos, GrupoMapper mapper, ReferenciasMiembro miembros, UbigeoService ubigeo,
			UsuarioActual usuarioActual) {
		this.grupos = grupos;
		this.mapper = mapper;
		this.miembros = miembros;
		this.ubigeo = ubigeo;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<GrupoResponse> listar() {
		return aRespuestas(grupos.findAllByOrderByNombreAsc());
	}

	@Transactional(readOnly = true)
	public GrupoResponse obtener(Long id) {
		return aRespuestas(List.of(buscar(id))).getFirst();
	}

	@Transactional
	public GrupoResponse crear(GuardarGrupoRequest solicitud) {
		DatosGrupo datos = datos(null, solicitud);
		if (nombreOcupado(datos.nombre(), null)) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
		Grupo grupo = guardar(Grupo.nuevo(datos));
		LOG.info("Grupo creado id={} por usuarioId={}", grupo.getId(), usuarioActual.id().orElse(null));
		return obtener(grupo.getId());
	}

	@Transactional
	public GrupoResponse actualizar(Long id, GuardarGrupoRequest solicitud) {
		Grupo grupo = buscar(id);
		DatosGrupo datos = datos(grupo, solicitud);
		if (nombreOcupado(datos.nombre(), id)) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
		grupo.actualizar(datos);
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

	private DatosGrupo datos(Grupo grupo, GuardarGrupoRequest solicitud) {
		miembros.validarAsignable(solicitud.liderId(), grupo == null ? null : grupo.getLiderId(), "liderId");
		miembros.validarAsignable(solicitud.anfitrionId(), grupo == null ? null : grupo.getAnfitrionId(),
				"anfitrionId");
		String distrito = Textos.normalizarOpcional(solicitud.distritoCodigo());
		ubigeo.validarDistrito(distrito, "distritoCodigo");
		return new DatosGrupo(Textos.normalizar(solicitud.nombre()), solicitud.liderId(), solicitud.anfitrionId(),
				solicitud.diaReunion(), solicitud.horaReunion(), Textos.normalizarOpcional(solicitud.direccion()),
				distrito, Textos.normalizarOpcional(solicitud.descripcion()));
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

	/** Pocos grupos por iglesia: se compara en Java para no depender de la configuración regional de la base. */
	private boolean nombreOcupado(String nombre, Long idExcluido) {
		return grupos.findAll()
			.stream()
			.anyMatch(grupo -> !grupo.getId().equals(idExcluido) && Textos.mismoTexto(grupo.getNombre(), nombre));
	}

	private Grupo buscar(Long id) {
		return grupos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	/** Escribe ya para que un nombre repetido por un alta simultánea responda 409 y no 500. */
	private Grupo guardar(Grupo grupo) {
		try {
			return grupos.saveAndFlush(grupo);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(NOMBRE_REPETIDO, "nombre");
		}
	}

}
