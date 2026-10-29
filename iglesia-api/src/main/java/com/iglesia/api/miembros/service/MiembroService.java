package com.iglesia.api.miembros.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.dto.EslabonCadenaResponse;
import com.iglesia.api.miembros.dto.FiltroMiembros;
import com.iglesia.api.miembros.dto.GuardarMiembroRequest;
import com.iglesia.api.miembros.dto.LideradoResponse;
import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.dto.MiembroResumenResponse;
import com.iglesia.api.miembros.dto.OpcionMiembroResponse;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.mapper.MiembroMapper;
import com.iglesia.api.miembros.model.AmbitoLectura;
import com.iglesia.api.miembros.model.ConteoPorLider;
import com.iglesia.api.miembros.model.DatosFicha;
import com.iglesia.api.miembros.repository.EspecificacionesMiembro;
import com.iglesia.api.miembros.repository.MiembroRepository;
import com.iglesia.api.niveles.service.NivelService;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

/**
 * Registro de miembros de la iglesia del token: ficha, jerarquía de liderazgo y estado. La lectura se limita al
 * {@link AmbitoMiembros ámbito} del usuario; quien no puede ver a un miembro recibe 404, como si no existiera.
 */
@Service
public class MiembroService {

	private static final Logger LOG = LoggerFactory.getLogger(MiembroService.class);

	static final String NO_ENCONTRADO = "Miembro no encontrado";

	static final String PROPIO_INVITADOR = "Un miembro no puede haberse invitado a sí mismo";

	static final String PROPIO_REGISTRADOR = "Un miembro no puede haberse registrado a sí mismo";

	static final String PASTOR_INACTIVO = "Un miembro desactivado no puede ser pastor principal";

	static final String PASTOR_SIMULTANEO = "Otro usuario designó al pastor principal al mismo tiempo. Vuelva a "
			+ "intentarlo";

	private static final Sort ORDEN_POR_NOMBRE = Sort.by("apellidos", "nombres", "id");

	private final MiembroRepository miembros;

	private final MiembroMapper mapper;

	private final AmbitoMiembros ambito;

	private final PoliticaJerarquia jerarquia;

	private final ReferenciasMiembro referencias;

	private final ConsultaGrupos grupos;

	private final NivelService niveles;

	private final UbigeoService ubigeo;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	MiembroService(MiembroRepository miembros, MiembroMapper mapper, AmbitoMiembros ambito,
			PoliticaJerarquia jerarquia, ReferenciasMiembro referencias, ConsultaGrupos grupos, NivelService niveles,
			UbigeoService ubigeo, IglesiaActual iglesiaActual, UsuarioActual usuarioActual) {
		this.miembros = miembros;
		this.mapper = mapper;
		this.ambito = ambito;
		this.jerarquia = jerarquia;
		this.referencias = referencias;
		this.grupos = grupos;
		this.niveles = niveles;
		this.ubigeo = ubigeo;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public Page<MiembroResumenResponse> listar(FiltroMiembros filtro, Pageable paginacion) {
		Specification<Miembro> criterio = Specification.allOf(EspecificacionesMiembro.dentroDe(ambito.actual()),
				EspecificacionesMiembro.nombreContiene(filtro.q()),
				EspecificacionesMiembro.conCondicion(filtro.condicion()),
				EspecificacionesMiembro.deNivel(filtro.nivelId()), EspecificacionesMiembro.deLider(filtro.liderId()),
				EspecificacionesMiembro.enUbigeo(filtro.ubigeo()), EspecificacionesMiembro.conEstado(filtro.activo()));
		Page<Miembro> pagina = miembros.findAll(criterio, paginacion);
		Map<String, UbicacionResponse> ubicaciones = ubicaciones(pagina.getContent());
		return pagina.map(miembro -> mapper.aResumen(miembro, ubicaciones));
	}

	@Transactional(readOnly = true)
	public MiembroResponse obtener(Long id) {
		Miembro miembro = miembros.findWithReferenciasById(id)
			.filter(encontrado -> ambito.actual().incluye(encontrado.getId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
		return aRespuesta(miembro);
	}

	/** Líderes del miembro, del directo al pastor principal. */
	@Transactional(readOnly = true)
	public List<EslabonCadenaResponse> cadena(Long id) {
		Miembro miembro = buscarVisible(id);
		List<Long> ids = miembros.idsDeLaCadena(iglesiaActual.requerida(), miembro.getId());
		Map<Long, Miembro> porId = miembros.findAllById(ids)
			.stream()
			.collect(Collectors.toMap(Miembro::getId, Function.identity()));
		return ids.stream().map(porId::get).filter(Objects::nonNull).map(mapper::aEslabon).toList();
	}

	/**
	 * Liderados directos activos del miembro, con su nivel y cuántos lidera cada uno, para expandir un nodo del árbol
	 * de liderazgo. Solo aparecen los que el usuario puede ver.
	 */
	@Transactional(readOnly = true)
	public List<LideradoResponse> liderados(Long id) {
		AmbitoLectura visible = ambito.actual();
		Miembro lider = buscarVisible(id);
		List<Miembro> directos = miembros.findByLiderIdAndActivoTrueOrderByApellidosAscNombresAscIdAsc(lider.getId())
			.stream()
			.filter(miembro -> visible.incluye(miembro.getId()))
			.toList();
		if (directos.isEmpty()) {
			return List.of();
		}
		Map<Long, Long> conteos = miembros.contarActivosPorLider(directos.stream().map(Miembro::getId).toList())
			.stream()
			.collect(Collectors.toMap(ConteoPorLider::liderId, ConteoPorLider::cantidad));
		return directos.stream()
			.map(miembro -> mapper.aLiderado(miembro, conteos.getOrDefault(miembro.getId(), 0L)))
			.toList();
	}

	/** Hasta {@value LimitesMiembro#OPCIONES_MAX} miembros activos del ámbito para el selector de miembro. */
	@Transactional(readOnly = true)
	public List<OpcionMiembroResponse> opciones(String q) {
		Specification<Miembro> criterio = Specification.allOf(EspecificacionesMiembro.dentroDe(ambito.actual()),
				EspecificacionesMiembro.nombreContiene(q), EspecificacionesMiembro.conEstado(true));
		return miembros.findAll(criterio, PageRequest.of(0, LimitesMiembro.OPCIONES_MAX, ORDEN_POR_NOMBRE))
			.map(mapper::aOpcion)
			.getContent();
	}

	/** Miembros con los mismos nombres y apellidos, sin distinguir mayúsculas ni espacios sobrantes. */
	@Transactional(readOnly = true)
	public List<MiembroResumenResponse> posiblesDuplicados(String nombres, String apellidos) {
		String buscados = Textos.normalizar(nombres);
		String apellidosBuscados = Textos.normalizar(apellidos);
		List<Miembro> coincidencias = miembros.buscarPorLargoDeNombre(buscados.length(), apellidosBuscados.length())
			.stream()
			.filter(miembro -> Textos.mismoTexto(miembro.getNombres(), buscados)
					&& Textos.mismoTexto(miembro.getApellidos(), apellidosBuscados))
			.toList();
		Map<String, UbicacionResponse> ubicaciones = ubicaciones(coincidencias);
		return coincidencias.stream().map(miembro -> mapper.aResumen(miembro, ubicaciones)).toList();
	}

	@Transactional
	public MiembroResponse crear(GuardarMiembroRequest solicitud) {
		Miembro miembro = miembros.save(Miembro.nuevo(datosFicha(null, solicitud)));
		LOG.info("Miembro creado id={} por usuarioId={}", miembro.getId(), usuarioActual.id().orElse(null));
		return aRespuesta(miembro);
	}

	@Transactional
	public MiembroResponse actualizar(Long id, GuardarMiembroRequest solicitud) {
		Miembro miembro = buscar(id);
		miembro.actualizar(datosFicha(miembro, solicitud));
		miembros.flush();
		LOG.info("Miembro actualizado id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return aRespuesta(miembro);
	}

	@Transactional
	public MiembroResponse cambiarEstado(Long id, boolean activo) {
		Miembro miembro = buscar(id);
		if (!activo && miembro.isActivo()) {
			jerarquia.validarDesactivacion(miembro);
		}
		miembro.cambiarEstado(activo);
		miembros.flush();
		LOG.info("Miembro {} id={} por usuarioId={}", activo ? "reactivado" : "desactivado", id,
				usuarioActual.id().orElse(null));
		return aRespuesta(miembro);
	}

	/** Marca al único pastor principal: se la quita al anterior y deja al nuevo sin líder. */
	@Transactional
	public MiembroResponse designarPastorPrincipal(Long id) {
		Miembro miembro = buscar(id);
		if (!miembro.isActivo()) {
			throw new ConflictoException(PASTOR_INACTIVO);
		}
		miembros.findByPastorPrincipalTrue()
			.filter(anterior -> !anterior.getId().equals(id))
			.ifPresent(anterior -> {
				anterior.quitarPastorPrincipal();
				// El índice único parcial exige quitar la marca antes de ponérsela al nuevo.
				miembros.flush();
			});
		miembro.designarPastorPrincipal();
		try {
			miembros.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(PASTOR_SIMULTANEO);
		}
		LOG.info("Pastor principal designado id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return aRespuesta(miembro);
	}

	/** Normaliza y valida la ficha y resuelve sus referencias. {@code miembro} es nulo en un alta. */
	private DatosFicha datosFicha(Miembro miembro, GuardarMiembroRequest solicitud) {
		validarNoSeReferenciaASiMismo(miembro, solicitud);
		String distrito = Textos.normalizarOpcional(solicitud.distritoCodigo());
		ubigeo.validarDistrito(distrito, "distritoCodigo");

		Miembro lider = referencias.asignable(solicitud.liderId(), actual(miembro, Miembro::getLider), "liderId");
		if (lider != null && (miembro == null || !Objects.equals(lider.getId(), idDe(miembro.getLider())))) {
			jerarquia.validarNuevoLider(miembro, lider);
		}
		grupos.validarAsignable(solicitud.grupoId(), miembro == null ? null : miembro.getGrupoId());
		return new DatosFicha(Textos.normalizar(solicitud.nombres()), Textos.normalizar(solicitud.apellidos()),
				solicitud.genero(), solicitud.fechaNacimiento(), mapper.anioEstimado(solicitud.edadAproximada()),
				solicitud.fechaIngreso(), solicitud.condicion(), solicitud.estadoCivil(),
				Textos.normalizarOpcional(solicitud.telefono()), Textos.normalizarOpcional(solicitud.correo()),
				Textos.normalizarOpcional(solicitud.direccion()), distrito,
				Textos.normalizarOpcional(solicitud.ocupacion()), Textos.normalizarOpcional(solicitud.profesion()),
				solicitud.nivelEstudios(), Textos.normalizarOpcional(solicitud.centroEstudios()),
				Textos.normalizarOpcional(solicitud.peticion()),
				niveles.asignable(solicitud.nivelId(), miembro == null ? null : miembro.getNivel()), lider,
				solicitud.grupoId(),
				referencias.asignable(solicitud.invitadoPorId(), actual(miembro, Miembro::getInvitadoPor),
						"invitadoPorId"),
				referencias.asignable(solicitud.registradoPorId(), actual(miembro, Miembro::getRegistradoPor),
						"registradoPorId"));
	}

	private void validarNoSeReferenciaASiMismo(Miembro miembro, GuardarMiembroRequest solicitud) {
		jerarquia.validarNoEsSuPropioLider(miembro, solicitud.liderId());
		if (miembro == null) {
			return;
		}
		if (miembro.getId().equals(solicitud.invitadoPorId())) {
			throw new ReglaNegocioException(PROPIO_INVITADOR, "invitadoPorId");
		}
		if (miembro.getId().equals(solicitud.registradoPorId())) {
			throw new ReglaNegocioException(PROPIO_REGISTRADOR, "registradoPorId");
		}
	}

	private static Long idDe(Miembro miembro) {
		return miembro == null ? null : miembro.getId();
	}

	private static Miembro actual(Miembro miembro, Function<Miembro, Miembro> referencia) {
		return miembro == null ? null : referencia.apply(miembro);
	}

	/** Ubicaciones de los distritos de los miembros, en una sola consulta. */
	private Map<String, UbicacionResponse> ubicaciones(List<Miembro> lista) {
		return ubigeo.ubicaciones(lista.stream().map(Miembro::getDistritoCodigo).toList());
	}

	private MiembroResponse aRespuesta(Miembro miembro) {
		ReferenciaResponse grupo = grupos.referencias(Collections.singleton(miembro.getGrupoId()))
			.get(miembro.getGrupoId());
		return mapper.aRespuesta(miembro, grupo, ubigeo.ubicacion(miembro.getDistritoCodigo()).orElse(null),
				miembros.countByLiderIdAndActivoTrue(miembro.getId()));
	}

	private Miembro buscarVisible(Long id) {
		AmbitoLectura visible = ambito.actual();
		return miembros.findById(id)
			.filter(miembro -> visible.incluye(miembro.getId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	private Miembro buscar(Long id) {
		return miembros.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

}
